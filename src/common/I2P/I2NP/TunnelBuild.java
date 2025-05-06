package common.I2P.I2NP;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InvalidObjectException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TunnelBuild extends I2NPMessage implements JSONSerializable {
    /**
     * Array list of the records which contain all info for the tunnel build
     */
    private ArrayList<Record> records;

    TunnelBuild(JSONArray json) throws InvalidObjectException {
        deserialize(json);
    }

    public TunnelBuild(ArrayList<Record> records) {
        this.records = new ArrayList<>();

        for (Record record : records) {
            this.records.add(record);
        }
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONArray))
            throw new InvalidObjectException("Must be JSONArray");

        JSONArray jsonArray = (JSONArray) jsonType;
        records = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getObject(i);
            Record record = new Record(jsonObject);
            records.add(record);
        }
    }

    @Override
    public JSONArray toJSONType() {
        JSONArray jsonArray = new JSONArray();
        for (Record record : records) {
            jsonArray.add(record.toJSONType());
        }
        return jsonArray;
    }

    
    public void decryptAES(SecretKey secretKey, byte[] iv) {
        for (Record record : records) {
            record.layeredDecrypt(secretKey, iv);
        }
    }

    public Record getRecord(int position) {
        return records.get(position);
    }

    public ArrayList<Record> getRecords() {
        return records;
    }

    public static class Record implements JSONSerializable {
        /**
         * First 16 butes of the SHA256 hash of the peer's RouterIdentity
         */
        private byte[] toPeer;
        /**
         * TunnelID to receives messages on
         */
        private int receiveTunnel;
        /**
         * 32 byte SHA256 hash of our RouterID
         */
        private byte[] ourIdent;
        /**
         * Next tunnelID for message
         */
        private int nextTunnel;
        /**
         * 32 byte SHA256 hash of RouterID
         */
        private byte[] nextIdent;
        /**
         * 32 byte AES key for layer encryption
         */
        private SecretKey layerKey;
        /**
         * 32 byte AES key for iv key
         */
        private SecretKey ivKey;
        /**
         * 32 byte AES key for replyKey
         */
        private SecretKey replyKey;
        /**
         * 16 byte reply iv
         */
        private byte[] replyIv;
        /**
         * Epoch time of request
         */
        private long requestTime;
        /**
         * ID of send message
         */
        private int sendMsgID;
        /**
         * encypted public key data under elgamal public key of peer
         */
        private byte[] encData; // not part of regular record but enc record

        /**
         * Type of tunnel object requested constructor
         */
        public enum TYPE {
            GATEWAY,
            PARTICIPANT,
            ENDPOINT
        };

        /**
         * Type of tunnel object requested
         */
        private TYPE type;
        /**
         * ArrayList of routerInfo for gateway node
         */
        private ArrayList<TunnelHopInfo> hopInfo;

        /**
         * Flag to indicate if this is a reply message
         */
        private boolean replyFlag;

        /**
         * Encrypted toPeer and replyKey
         */
        private byte[] encToPeer;
        private byte[] encReplyKey;

        /**
         * Construct a record from a json
         * 
         * @param jsonObject JSON to deserialize
         * @throws InvalidObjectException throws if json is invalid
         */
        public Record(JSONObject jsonObject) throws InvalidObjectException {
            deserialize(jsonObject);
        }

        /**
         * Constructs a new {@code Record} instance by copying the fields from the
         * given encrypted record.
         * NOTE: This constructor is for encrypted records only.
         * 
         * @param encRecord
         */
        public Record(Record encRecord) {
            this.encToPeer = encRecord.encToPeer;
            this.encReplyKey = encRecord.encReplyKey;
            this.replyIv = encRecord.replyIv;
            this.encData = encRecord.encData;
        }

        /**
         * Constructs a new {@code Record} instance with all required fields.
         *
         * @param toPeer        The first 16 bytes of the SHA-256 hash of the peer's
         *                      RouterIdentity.
         * @param receiveTunnel The tunnel ID to receive messages on.
         * @param ourIdent      The 32-byte SHA-256 hash of our RouterIdentity.
         * @param nextTunnel    The next tunnel ID for forwarding the message.
         * @param nextIdent     The 32-byte SHA-256 hash of the next RouterIdentity.
         * @param layerKey      The 32-byte AES key used for layer encryption.
         * @param ivKey         The 32-byte AES key used for IV derivation.
         * @param replyKey      The 32-byte AES key used for reply encryption.
         * @param replyIv       The 16-byte IV used for reply messages.
         * @param requestTime   The epoch time (in seconds) when the request was made.
         * @param sendMsgID     The ID of the sent message.
         * @param type          The type of the tunnel object requested.
         * @param hopInfo       An ArrayList of TunnelHopInfo objects representing the
         *                      hops in the tunnel.
         * @param replyFlag     A flag indicating if this is a reply message.
         */
        public Record(byte[] toPeer, int receiveTunnel, byte[] ourIdent, int nextTunnel, byte[] nextIdent,
                SecretKey layerKey,
                SecretKey ivKey, SecretKey replyKey, byte[] replyIv, long requestTime, int sendMsgID, TYPE type,
                ArrayList<TunnelHopInfo> hopInfo, boolean replyFlag) {
            this.toPeer = toPeer;
            this.receiveTunnel = receiveTunnel;
            this.ourIdent = ourIdent;
            this.nextTunnel = nextTunnel;
            this.nextIdent = nextIdent;
            this.layerKey = layerKey;
            this.ivKey = ivKey;
            this.replyKey = replyKey;
            this.replyIv = replyIv;
            this.requestTime = requestTime;
            this.sendMsgID = sendMsgID;
            this.type = type;
            this.hopInfo = hopInfo;
            this.replyFlag = replyFlag;
        }

        /**
         * Constructs a new {@code Record} instance with the specified toPeer and
         * encData.
         *
         * @param toPeer  The first 16 bytes of the SHA-256 hash of the peer's
         *                RouterIdentity.
         * @param encData The encrypted data associated with this record.
         */
        public Record(byte[] encToPeer, byte[] encReplyKey, byte[] replyIV, byte[] encData) {
            this.encToPeer = encToPeer;
            this.encReplyKey = encReplyKey;
            this.replyIv = replyIV; // regular
            this.encData = encData;
        }

        /**
         * Encrypts the record using a hybrid encryption scheme. It uses ElGamal to
         * encrypt the toPeer, replyKey, and replyIV fields, and AES to encrypt the rest
         * of the record.
         * 
         * @param elgamalPublicKey
         * @param aesKey
         */
        public void hybridEncrypt(PublicKey elgamalPublicKey, SecretKey aesKey) {
            try {
                // Encrypt the toPeer, replyKey, and replyIV fields using ElGamal
                Cipher elgamalCipher = Cipher.getInstance("ElGamal/None/NoPadding");
                elgamalCipher.init(Cipher.ENCRYPT_MODE, elgamalPublicKey);

                this.encToPeer = elgamalCipher.doFinal(this.toPeer);
                this.encReplyKey = elgamalCipher.doFinal(this.replyKey.getEncoded());
                this.replyIv = this.replyIv; // OoOoooOOoooOooo keep it unencrypteddddd - the ghost of Sam's past whos
                                             // made too many mistakes

                // byte[] encryptedReplyIv = elgamalCipher.doFinal(this.replyIv);

                this.toPeer = null; // Clear the toPeer field after encryption just in case
                this.replyKey = null; // Clear the replyKey field after encryption just in case

                // Encrypt the remaining fields using AES
                Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(128, this.replyIv);
                aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

                JSONObject remainingFeilds = new JSONObject();
                remainingFeilds.put("receiveTunnel", receiveTunnel);
                remainingFeilds.put("ourIdent", Base64.toBase64String(ourIdent));
                remainingFeilds.put("nextTunnel", nextTunnel);
                remainingFeilds.put("nextIdent", Base64.toBase64String(nextIdent));
                remainingFeilds.put("layerKey", Base64.toBase64String(layerKey.getEncoded()));
                remainingFeilds.put("IVKey", Base64.toBase64String(ivKey.getEncoded()));
                remainingFeilds.put("requestTime", requestTime);
                remainingFeilds.put("sendMsgID", sendMsgID);
                remainingFeilds.put("type", type.ordinal());
                remainingFeilds.put("replyFlag", replyFlag);

                JSONArray hopInfoArray = new JSONArray();
                if (hopInfo != null) {
                    for (TunnelHopInfo hop : hopInfo) {
                        hopInfoArray.add(hop.toJSONType());
                    }
                }
                remainingFeilds.put("hopInfo", hopInfoArray);

                byte[] aesEncryptedData = aesCipher
                        .doFinal(remainingFeilds.toJSON().getBytes(StandardCharsets.UTF_8));

                this.encData = aesEncryptedData; // Store the encrypted data in encData
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                    | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException("Encryption error: " + e.getMessage(), e);
            }
        }

        // More or less an example for seth, should we have individual encrypt and
        // decrypt
        // method for each chunk?
        /**
         * Elgamal encrypts only the toPeer, replyKey, and replyIV fields.
         *
         * @param elgamalPublicKey The public key to use for ElGamal encryption.
         */
        public void elgamalEncrypt(PublicKey elgamalPublicKey) {
            try {
                // Encrypt the toPeer, replyKey, and replyIV fields using ElGamal
                Cipher elgamalCipher = Cipher.getInstance("ElGamal/None/NoPadding");
                elgamalCipher.init(Cipher.ENCRYPT_MODE, elgamalPublicKey);

                byte[] encryptedToPeer = elgamalCipher.doFinal(this.toPeer);
                byte[] encryptedReplyKey = elgamalCipher.doFinal(this.replyKey.getEncoded());
                byte[] encryptedReplyIv = elgamalCipher.doFinal(this.replyIv);

                // Store the encrypted data in a JSON object
                JSONObject encryptedData = new JSONObject();
                encryptedData.put("toPeer", Base64.toBase64String(encryptedToPeer));
                encryptedData.put("replyKey", Base64.toBase64String(encryptedReplyKey));
                encryptedData.put("replyIv", Base64.toBase64String(encryptedReplyIv));

                // Save to enc data as a byte array
                this.encData = encryptedData.toJSON().getBytes(StandardCharsets.UTF_8);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                    | IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException("Encryption error: " + e.getMessage(), e);
            }
        }

        /**
         * Decrypts the record using a hybrid encryption scheme. It uses ElGamal to
         * decrypt the toPeer, replyKey fields, and AES to decrypt the rest of the
         * record.
         *
         * @param elgamalPrivateKey The ElGamal private key for decrypting the toPeer
         *                          and replyKey fields.
         * @param aesKey            The AES key for decrypting the remaining fields.
         */
        public void hybridDecrypt(PrivateKey elgamalPrivateKey) {
            try {
                // Decrypt the toPeer and replyKey fields using ElGamal
                Cipher elgamalCipher = Cipher.getInstance("ElGamal/None/NoPadding");
                elgamalCipher.init(Cipher.DECRYPT_MODE, elgamalPrivateKey);

                this.toPeer = elgamalCipher.doFinal(this.encToPeer);
                byte[] decryptedReplyKey = elgamalCipher.doFinal(this.encReplyKey);
                this.replyKey = new SecretKeySpec(decryptedReplyKey, "AES");
                this.replyIv = this.replyIv; // Keep replyIv as it is since it wasn't encrypted

                System.out.println("Decrypted toPeer: " + Base64.toBase64String(this.toPeer));
                System.out.println("Decrypted replyKey: " + Base64.toBase64String(this.replyKey.getEncoded()));

                // Decrypt the remaining fields using AES
                Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(128, this.replyIv);
                aesCipher.init(Cipher.DECRYPT_MODE, this.replyKey, gcmSpec);

                byte[] aesDecryptedData = aesCipher.doFinal(this.encData);

                // Deserialize the decrypted data into a JSON object
                JSONObject decryptedJson = JsonIO.readObject(new String(aesDecryptedData, StandardCharsets.UTF_8));
                this.receiveTunnel = decryptedJson.getInt("receiveTunnel");
                this.ourIdent = Base64.decode(decryptedJson.getString("ourIdent"));
                this.nextTunnel = decryptedJson.getInt("nextTunnel");
                this.nextIdent = Base64.decode(decryptedJson.getString("nextIdent"));
                this.layerKey = new SecretKeySpec(Base64.decode(decryptedJson.getString("layerKey")), "AES");
                this.ivKey = new SecretKeySpec(Base64.decode(decryptedJson.getString("IVKey")), "AES");
                this.requestTime = decryptedJson.getLong("requestTime");
                this.sendMsgID = decryptedJson.getInt("sendMsgID");
                this.type = TYPE.values()[decryptedJson.getInt("type")];
                this.replyFlag = decryptedJson.getBoolean("replyFlag");

                this.hopInfo = new ArrayList<>();
                JSONArray hopInfoArray = decryptedJson.getArray("hopInfo");
                for (int i = 0; i < hopInfoArray.size(); i++) {
                    this.hopInfo.add(new TunnelHopInfo(hopInfoArray.getObject(i)));
                }

                // Clear the encrypted fields after decryption
                this.encToPeer = null;
                this.encReplyKey = null;
                this.encData = null; // Clear the encData field after decryption just in case

            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                    | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException("Decryption error: " + e.getMessage(), e);
            }
        }

        // not sure what to do with two peer in these bad boys - we can come back to
        // this

        /**
         * Encrypts the record using AES/GCM/NoPadding with the provided key and IV.
         * Encrypts encToPeer, encReplyKey, and encData separately, each with its own
         * cipher instance.
         *
         * @param key AES key to use
         * @param iv  12-byte IV (should ideally vary for each encryption in GCM mode)
         */
        public void layeredEncrypt(SecretKey key, byte[] iv) {
            try {
                // Encrypt encToPeer
                Cipher enc1 = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec1 = new GCMParameterSpec(128, iv);
                enc1.init(Cipher.ENCRYPT_MODE, key, gcmSpec1);
                encToPeer = enc1.doFinal(encToPeer);

                // Encrypt encReplyKey
                Cipher enc2 = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec2 = new GCMParameterSpec(128, iv); // you know i should probably vary the iv but
                                                                           // i also havent been outside in three days
                                                                           // and if i have to write another method i
                                                                           // might scream
                enc2.init(Cipher.ENCRYPT_MODE, key, gcmSpec2);
                encReplyKey = enc2.doFinal(encReplyKey);

                // Encrypt encData
                Cipher enc3 = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec3 = new GCMParameterSpec(128, iv);
                enc3.init(Cipher.ENCRYPT_MODE, key, gcmSpec3);
                encData = enc3.doFinal(encData);

            } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException
                    | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
                throw new RuntimeException("Layered encryption failed", e);
            }
        }

        // Lord please let all of these encryption methods work
        // St. Isidore of Seville please guide my hand as my Patron Saint

        /**
         * Decrypts the record using AES/GCM/NoPadding with the provided key and IV.
         * Decrypts encToPeer, encReplyKey, and encData separately.
         *
         * @param key AES key to use
         * @param iv  12-byte IV used during encryption
         */
        public void layeredDecrypt(SecretKey key, byte[] iv) {
            try {
                // Decrypt encToPeer
                Cipher dec1 = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec1 = new GCMParameterSpec(128, iv);
                dec1.init(Cipher.DECRYPT_MODE, key, gcmSpec1);
                encToPeer = dec1.doFinal(encToPeer);

                // Decrypt encReplyKey
                Cipher dec2 = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec2 = new GCMParameterSpec(128, iv);
                dec2.init(Cipher.DECRYPT_MODE, key, gcmSpec2);
                encReplyKey = dec2.doFinal(encReplyKey);

                // Decrypt encData
                Cipher dec3 = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec3 = new GCMParameterSpec(128, iv);
                dec3.init(Cipher.DECRYPT_MODE, key, gcmSpec3);
                encData = dec3.doFinal(encData);

            } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException
                    | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
                throw new RuntimeException("Layered decryption failed", e);
            }
        }

        @Override
        public void deserialize(JSONType jsonType) throws InvalidObjectException {
            if (!(jsonType instanceof JSONObject))
                throw new InvalidObjectException("Must be JSONObject");

            JSONObject json = (JSONObject) jsonType;
            json.checkValidity(new String[] { "replyIV" });

            if (json.getString("encToPeer") != null) {
                this.encToPeer = Base64.decode(json.getString("encToPeer"));
                this.encReplyKey = Base64.decode(json.getString("encReplyKey"));
            } else {
                this.toPeer = Base64.decode(json.getString("toPeer"));
                this.replyKey = new SecretKeySpec(Base64.decode(json.getString("replyKey")), "AES");
            }

            this.replyIv = Base64.decode(json.getString("replyIV"));

            // Check if encData is a JSON object or a Base64 string
            if (json.get("encData") instanceof JSONObject) {
                JSONObject encDataJSON = json.getObject("encData");

                this.receiveTunnel = encDataJSON.getInt("receiveTunnel");
                this.ourIdent = Base64.decode(encDataJSON.getString("ourIdent"));
                this.nextTunnel = encDataJSON.getInt("nextTunnel");
                this.nextIdent = Base64.decode(encDataJSON.getString("nextIdent"));
                this.layerKey = new SecretKeySpec(Base64.decode(encDataJSON.getString("layerKey")), "AES");
                this.ivKey = new SecretKeySpec(Base64.decode(encDataJSON.getString("IVKey")), "AES");
                this.requestTime = encDataJSON.getLong("requestTime");
                this.sendMsgID = encDataJSON.getInt("sendMsgID");
                this.type = TYPE.values()[encDataJSON.getInt("type")];

                // Deserialize hopInfo
                JSONArray hopInfoArray = encDataJSON.getArray("hopInfo");
                this.hopInfo = new ArrayList<>();
                for (int i = 0; i < hopInfoArray.size(); i++) {
                    this.hopInfo.add(new TunnelHopInfo(hopInfoArray.getObject(i)));
                }

                this.replyFlag = encDataJSON.getBoolean("replyFlag");
            } else {
                this.encData = Base64.decode(json.getString("encData"));
            }
        }

        private void setEncData(byte[] encData) {
            this.encData = encData;
        }

        @Override
        public JSONObject toJSONType() {
            JSONObject json = new JSONObject();
            if (encToPeer != null) {
                json.put("encToPeer", Base64.toBase64String(encToPeer));
                json.put("encReplyKey", Base64.toBase64String(encReplyKey));
            } else {
                json.put("toPeer", Base64.toBase64String(toPeer));
                json.put("replyKey", Base64.toBase64String(replyKey.getEncoded()));
            }

            json.put("replyIV", Base64.toBase64String(replyIv));

            if (encData != null) {
                json.put("encData", Base64.toBase64String(encData));
            } else {
                JSONObject encDataJSON = new JSONObject();
                encDataJSON.put("receiveTunnel", receiveTunnel);
                encDataJSON.put("ourIdent", Base64.toBase64String(ourIdent));
                encDataJSON.put("nextTunnel", nextTunnel);
                encDataJSON.put("nextIdent", Base64.toBase64String(nextIdent));
                encDataJSON.put("layerKey", Base64.toBase64String(layerKey.getEncoded()));
                encDataJSON.put("IVKey", Base64.toBase64String(ivKey.getEncoded()));
                encDataJSON.put("requestTime", requestTime);
                encDataJSON.put("sendMsgID", sendMsgID);
                encDataJSON.put("type", type.ordinal());
                encDataJSON.put("replyFlag", replyFlag);

                JSONArray hopInfoArray = new JSONArray();
                for (TunnelHopInfo hop : hopInfo) {
                    hopInfoArray.add(hop.toJSONType());
                }
                encDataJSON.put("hopInfo", hopInfoArray);

                json.put("encData", encDataJSON);
            }
            return json;
        }

        public byte[] getEncData() {
            return encData;
        }

        public byte[] getToPeer() {
            return toPeer;
        }

        public int getReceiveTunnel() {
            return receiveTunnel;
        }

        public byte[] getOurIdent() {
            return ourIdent;
        }

        public int getNextTunnel() {
            return nextTunnel;
        }

        public byte[] getNextIdent() {
            return nextIdent;
        }

        public SecretKey getLayerKey() {
            return layerKey;
        }

        public SecretKey getIvKey() {
            return ivKey;
        }

        public SecretKey getReplyKey() {
            return replyKey;
        }

        public byte[] getReplyIv() {
            return replyIv;
        }

        public long getRequestTime() {
            return requestTime;
        }

        public int getSendMsgID() {
            return sendMsgID;
        }

        public TYPE getPosition() {
            return type;
        }

        public ArrayList<TunnelHopInfo> getHopInfo() {
            return hopInfo;
        }

        public boolean getReplyFlag() {
            return replyFlag;
        }
    }
}
