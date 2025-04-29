package common.I2P.I2NP;

import common.I2P.tunnels.TunnelManager;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

public class TunnelBuild extends I2NPMessage implements JSONSerializable {
    /**
     * Records key is toPeer 16 byte SHA256 hash of peerID with value being record
     */
    private HashMap<String, Record> records;

    TunnelBuild(JSONObject json) throws InvalidObjectException {
        deserialize(json);
    }

    public TunnelBuild(List<Record> records) {
        this.records = new HashMap<>();

        for (Record record : records) {
            this.records.put(Base64.toBase64String(record.getToPeer()), record);
        }
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONArray))
            throw new InvalidObjectException("Must be JSONArray");

        JSONArray jsonArray = (JSONArray) jsonType;

        for (int i = 0; i < jsonArray.size(); i++) {
            Record record = new Record(jsonArray.getObject(i));

            records.put(Base64.toBase64String(record.getToPeer()), record);
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
        private byte[] encData;

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
         */
        public Record(byte[] toPeer, int receiveTunnel, byte[] ourIdent, int nextTunnel, byte[] nextIdent,
                SecretKey layerKey,
                SecretKey ivKey, SecretKey replyKey, byte[] replyIv, long requestTime, int sendMsgID, TYPE type) {
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
        }

        public Record(byte[] toPeer, byte[] encData) {
            this.toPeer = toPeer;
            this.encData = encData;
        }

        @Override
        public void deserialize(JSONType jsonType) throws InvalidObjectException {
            if (!(jsonType instanceof JSONObject))
                throw new InvalidObjectException("Must be JSONObject");

            JSONObject json = (JSONObject) jsonType;
            json.checkValidity(new String[] { "toPeer", "encData" });

            this.toPeer = Base64.decode(json.getString("toPeer"));
            this.encData = Base64.decode(json.getString("encData"));
            // todo deal with deserializing encypted might want to add seperate method since
            // only peer with elGamal private key can undo the encryption to get valid JSON
        }

        @Override
        public JSONType toJSONType() {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("toPeer", Base64.toBase64String(toPeer));

            if (encData == null) {
                JSONObject encDataJSON = new JSONObject();

                // not sure if we should handle this like this or turn Tunnels into serializable
                encDataJSON.put("receiveTunnel", receiveTunnel);
                encDataJSON.put("ourIdent", Base64.toBase64String(ourIdent));
                encDataJSON.put("nextTunnel", nextTunnel);
                encDataJSON.put("nextIdent", Base64.toBase64String(nextIdent));
                encDataJSON.put("layerKey", Base64.toBase64String(layerKey.getEncoded()));
                encDataJSON.put("IVKey", Base64.toBase64String(ivKey.getEncoded()));
                encDataJSON.put("replyKey", Base64.toBase64String(replyKey.getEncoded()));
                encDataJSON.put("replyIV", Base64.toBase64String(replyIv));
                encDataJSON.put("requestTime", requestTime);
                encDataJSON.put("sendMsgID", sendMsgID);
                encDataJSON.put("type", type.toString());

                jsonObject.put("encData", encDataJSON.toJSON());
            } else {
                // todo this might be easier
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

        public Record encrypt(PublicKey publicKey) {
            try {
                // Generate AES session key
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256); // 256-bit AES key
                SecretKey sessionKey = keyGen.generateKey();

                // Encrypt fields using sessionKey (AES)
                Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // ECB OK for single blocks
                aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey);

                // Encrypt each field
                byte[] encryptedLayerKey = aesCipher.doFinal(layerKey.getEncoded());
                byte[] encryptedIvKey = aesCipher.doFinal(ivKey.getEncoded());
                byte[] encryptedReplyKey = aesCipher.doFinal(replyKey.getEncoded());
                byte[] encryptedReplyIv = aesCipher.doFinal(replyIv);
                byte[] encryptedNextIdent = aesCipher.doFinal(nextIdent);
                byte[] encryptedOurIdent = aesCipher.doFinal(ourIdent);

                // Encrypt session key using ElGamal
                Cipher elgamalCipher = Cipher.getInstance("ElGamal/None/NoPadding", "BC");
                elgamalCipher.init(Cipher.ENCRYPT_MODE, publicKey);
                byte[] encryptedSessionKey = elgamalCipher.doFinal(sessionKey.getEncoded());

                // Package everything
                JSONObject encDataJSON = new JSONObject();
                encDataJSON.put("encryptedSessionKey", Base64.toBase64String(encryptedSessionKey));
                encDataJSON.put("encryptedLayerKey", Base64.toBase64String(encryptedLayerKey));
                encDataJSON.put("encryptedIvKey", Base64.toBase64String(encryptedIvKey));
                encDataJSON.put("encryptedReplyKey", Base64.toBase64String(encryptedReplyKey));
                encDataJSON.put("encryptedReplyIv", Base64.toBase64String(encryptedReplyIv));
                encDataJSON.put("encryptedNextIdent", Base64.toBase64String(encryptedNextIdent));
                encDataJSON.put("encryptedOurIdent", Base64.toBase64String(encryptedOurIdent));
                // Also store plaintext ints
                encDataJSON.put("receiveTunnel", receiveTunnel);
                encDataJSON.put("nextTunnel", nextTunnel);
                encDataJSON.put("requestTime", requestTime);
                encDataJSON.put("sendMsgID", sendMsgID);
                encDataJSON.put("type", type.toString());

                this.encData = encDataJSON.toJSON().getBytes();

                return new Record(this.toPeer, this.encData);
            } catch (Exception e) {
                System.err.println("Encryption failed: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }
}
