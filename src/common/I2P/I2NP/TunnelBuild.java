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
     * Records key is toPeer 16 byte SHA256 hash of peerID with value being record
     */
    private HashMap<String, Record> records;

    TunnelBuild(JSONArray json) throws InvalidObjectException {
        deserialize(json);
    }

    public TunnelBuild(ArrayList<Record> records) {
        this.records = new HashMap<>();

        for (Record record : records) {
            this.records.put(Base64.toBase64String(record.getToPeer()), record);
        }
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (jsonType == null) {
            throw new InvalidObjectException("TunnelBuild deserialization failed: input JSONType is null");
        }

        if (!(jsonType instanceof JSONArray)) {
            throw new InvalidObjectException("TunnelBuild deserialization failed: Expected JSONArray, got "
                    + jsonType.getClass().getSimpleName());
        }

        JSONArray jsonArray = (JSONArray) jsonType;
        this.records = new HashMap<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject recordObject = jsonArray.getObject(i);
            Record record = new Record(recordObject);
            String key = Base64.toBase64String(record.getToPeer());
            records.put(key, record);
        }
    }

    @Override
    public JSONArray toJSONType() {
        JSONArray jsonArray = new JSONArray();
        for (Record record : records.values()) {
            jsonArray.add(record.toJSONType());
        }
        return jsonArray;
    }

    public void decryptAES(SecretKey secretKey) {
        for (Record record : records.values()) {
            try {
                Cipher dec = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(128, record.replyIv);
                dec.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

                byte[] decByte = dec.doFinal(record.getEncData());
                record.setEncData(decByte);

            } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchAlgorithmException
                    | IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException(e); // should not hit case
            } catch (InvalidKeyException e) {
                throw new IllegalArgumentException("bad key " + e);
            }
        }
    }

    public Record getRecord(String key) {
        return records.get(key);
    }

    public List<Record> getRecords() {
        return new ArrayList<>(records.values());
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
         * Construct a record from a json
         * 
         * @param jsonObject JSON to deserialize
         * @throws InvalidObjectException throws if json is invalid
         */
        public Record(JSONObject jsonObject) throws InvalidObjectException {
            deserialize(jsonObject);
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
        public Record(byte[] toPeer, byte[] encData) {
            this.toPeer = toPeer; // uhhh... maybe uneeded?
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

                byte[] encryptedToPeer = elgamalCipher.doFinal(this.toPeer);
                byte[] encryptedReplyKey = elgamalCipher.doFinal(this.replyKey.getEncoded());
                byte[] encryptedReplyIv = elgamalCipher.doFinal(this.replyIv);

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
                        .doFinal(remainingFeilds.toString().getBytes(StandardCharsets.UTF_8));

                // Create a JSON object to hold the encrypted data
                JSONObject encryptedData = new JSONObject();
                encryptedData.put("encryptedToPeer", Base64.toBase64String(encryptedToPeer));
                encryptedData.put("encryptedReplyKey", Base64.toBase64String(encryptedReplyKey));
                encryptedData.put("encryptedReplyIv", Base64.toBase64String(encryptedReplyIv));
                encryptedData.put("encryptedData", Base64.toBase64String(aesEncryptedData));

                // Store the encrypted data as a byte array
                this.encData = encryptedData.toString().getBytes(StandardCharsets.UTF_8);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                    | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException("Encryption error: " + e.getMessage(), e);
            }
        }

        // More or less an example for seth, should we have individual encrypt and decrypt
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
                encryptedData.put("encryptedToPeer", Base64.toBase64String(encryptedToPeer));
                encryptedData.put("encryptedReplyKey", Base64.toBase64String(encryptedReplyKey));
                encryptedData.put("encryptedReplyIv", Base64.toBase64String(encryptedReplyIv));

                // Save to enc data as a byte array
                this.encData = encryptedData.toJSON().getBytes(StandardCharsets.UTF_8);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                    | IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException("Encryption error: " + e.getMessage(), e);
            }
        }


        /**
         * Decrypts the record using a hybrid decryption scheme. It uses ElGamal to
         * decrypt the toPeer, replyKey, and replyIV fields, and AES to decrypt the rest
         * of the record. Extracts the AES key and IV from the encrypted data.
         *
         * @param elgamalPrivateKey The private key to use for ElGamal decryption.
         */
        public void hybridDecrypt(PrivateKey elgamalPrivateKey) {
            try {
                // System.out.println("Encrypted data: " + new String(encData, StandardCharsets.UTF_8));
                JSONObject encryptedData = JsonIO.readObject(new String(encData, StandardCharsets.UTF_8));
                Cipher elgamalCipher = Cipher.getInstance("ElGamal/None/NoPadding");
                elgamalCipher.init(Cipher.DECRYPT_MODE, elgamalPrivateKey);

                this.toPeer = elgamalCipher.doFinal(Base64.decode(encryptedData.getString("encryptedToPeer")));
                this.replyKey = new SecretKeySpec(
                        elgamalCipher.doFinal(Base64.decode(encryptedData.getString("encryptedReplyKey"))), "AES");
                this.replyIv = elgamalCipher.doFinal(Base64.decode(encryptedData.getString("encryptedReplyIv")));

                Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(128, this.replyIv);
                aesCipher.init(Cipher.DECRYPT_MODE, this.replyKey, gcmSpec);

                byte[] aesDecryptedData = aesCipher.doFinal(Base64.decode(encryptedData.getString("encryptedData")));
                JSONObject remainingFields = JsonIO.readObject(new String(aesDecryptedData, StandardCharsets.UTF_8));

                this.receiveTunnel = remainingFields.getInt("receiveTunnel");
                this.ourIdent = Base64.decode(remainingFields.getString("ourIdent"));
                this.nextTunnel = remainingFields.getInt("nextTunnel");
                this.nextIdent = Base64.decode(remainingFields.getString("nextIdent"));
                this.layerKey = new SecretKeySpec(
                        Base64.decode(remainingFields.getString("layerKey")), "AES");
                this.ivKey = new SecretKeySpec(Base64.decode(remainingFields.getString("IVKey")), "AES");
                this.requestTime = remainingFields.getLong("requestTime");
                this.sendMsgID = remainingFields.getInt("sendMsgID");
                this.type = TYPE.values()[remainingFields.getInt("type")];
                this.replyFlag = remainingFields.getBoolean("replyFlag");

                this.hopInfo = new ArrayList<>();
                JSONArray hopInfoArray = remainingFields.getArray("hopInfo");
                for (int i = 0; i < hopInfoArray.size(); i++) {
                    this.hopInfo.add(new TunnelHopInfo(hopInfoArray.getObject(i)));
                }

                this.encData = null; // Clear the encData field after decryption just in case

            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
                    | BadPaddingException | InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }

        }

        // not sure what to do with two peer in these bad boys -  we can come back to this

        /**
         * Encrypts the record using AES encryption with the provided key and IV.
         * Just the entire encData chunk.
         * 
         * @param key
         * @param iv
         */
        public void layeredEncrypt(SecretKey key, byte[] iv) {
            try {
                Cipher enc = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv); // we are not using this record iv
                enc.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
                encData = enc.doFinal(this.serialize().getBytes(StandardCharsets.UTF_8));
            } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException
                    | NoSuchAlgorithmException | BadPaddingException e) {
                throw new RuntimeException(e); // should not hit case
            } catch (InvalidKeyException e) {
                throw new IllegalArgumentException("bad key " + e);
            }
        }
               
        // Lord please let all of these encryption methods work
        // St. Isidore of Seville please guide my hand as my Patron Saint

        /**
         * Decrypts the record using AES decryption with the provided key and IV.
         * 
         * @param key
         * @param iv
         */
        public void layeredDecrypt(SecretKey key) {
            try {
                Cipher dec = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(128, this.replyIv);
                dec.init(Cipher.DECRYPT_MODE, key, gcmSpec);
                byte[] decByte = dec.doFinal(encData);
                encData = decByte; // overwrite this encData with the now stripped copy of data
                // deserialize(JsonIO.readObject(new String(decByte, StandardCharsets.UTF_8)));
            } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException
                    | NoSuchAlgorithmException | BadPaddingException e) {
                throw new RuntimeException(e); // should not hit case
            } catch (InvalidKeyException e) {
                throw new IllegalArgumentException("bad key " + e);
            }
        }


        @Override
        public void deserialize(JSONType jsonType) throws InvalidObjectException {
            if (!(jsonType instanceof JSONObject))
                throw new InvalidObjectException("Must be JSONObject");

            JSONObject json = (JSONObject) jsonType;
            json.checkValidity(new String[] { "toPeer", "encData", "replyIV", "replyKey" });

            this.toPeer = Base64.decode(json.getString("toPeer"));
            this.replyIv = Base64.decode(json.getString("replyIV"));
            this.replyKey = new SecretKeySpec(Base64.decode(json.getString("replyKey")), "AES");
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
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("toPeer", Base64.toBase64String(toPeer));
            jsonObject.put("replyIV", Base64.toBase64String(replyIv));
            jsonObject.put("replyKey", Base64.toBase64String(replyKey.getEncoded()));
            if (encData == null) {
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

                // Serialize hopInfo
                JSONArray hopInfoArray = new JSONArray();
                if (hopInfo != null) {
                    for (TunnelHopInfo hop : hopInfo) {
                        hopInfoArray.add(hop.toJSONType());
                    }
                }
                encDataJSON.put("hopInfo", hopInfoArray);

                encDataJSON.put("replyFlag", replyFlag);

                jsonObject.put("encData", encDataJSON);
            } else {
                jsonObject.put("encData", Base64.toBase64String(encData));
            }

            return jsonObject;
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
