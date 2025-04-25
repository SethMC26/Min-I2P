package common.I2P.I2NP;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class I2NPHeader implements JSONSerializable {
    /**
     * Types of supported I2NP Messages
     */
    public enum TYPE {
        DATABASESTORE(1),
        DATABASELOOKUP(2),
        DATABASESEARCHREPLY(3),
        DELIVERYSTATUS(10),
        TUNNELBUILD(21),
        TUNNELBUILDREPLY(22);

        //below code courtesy of chatGPT
        private final int value;

        TYPE(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static TYPE fromValue(int value) {
            for (TYPE type : TYPE.values()) {
                if (type.getValue() == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown value: " + value);
        }
    }

    /**
     * Type of message
     */
    private TYPE type;
    /**
     * Uniquely ID messages
     */
    private int msgID;
    /**
     * Epoch time when message expires
     */
    private Long expiration;
    /**
     * Checksum of payload SHA256 hash truncated to three byte(use 3 bytes to avoid redudant padding on base64 encoding)
     * @apiNote I2P spec has this as 1 byte, but we are using base64 strings to send this so we are using 3 bytes since
     * base64 encoding is 6 bit aligned(no padding bits at 3 bytes)
     */
    private byte[] chks;
    /**
     * Message for this I2NP header
     */
    private I2NPMessage message;

    /**
     *
     * @param type TYPE of message
     * @param msgID Int of unique ID for message
     * @param expiration Epoch time when message will expire
     * @param message message to send
     */
    public I2NPHeader(TYPE type, int msgID, Long expiration, I2NPMessage message) {
        this.type = type;
        this.msgID = msgID;
        this.expiration = expiration;

        this.message = message;

        try
        {
            //hash payload of message
            MessageDigest md = MessageDigest.getInstance("SHA256");
            md.update(message.serialize().getBytes(StandardCharsets.UTF_8));
            //take only first 3 bytes of hash(I2P spec uses 1 but we are using base64 encoding is 6 bit aligned
            // So 3 bytes is the smallest we can have without padding bytes (24 bits divides evenly into 6)
            chks = Arrays.copyOfRange(md.digest(), 0, 3);

        }
        catch (NoSuchAlgorithmException ex)
        {
            //should not hit this case
           throw new RuntimeException(ex);
        }
    }

    I2NPHeader(JSONObject json) throws InvalidObjectException {
        deserialize(json);
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject messageJSON = new JSONObject();

        messageJSON.put("type", type.getValue());
        messageJSON.put("msgID", msgID);
        messageJSON.put("expiration", expiration);
        messageJSON.put("chks", Base64.toBase64String(chks));
        messageJSON.put("message", message.toJSONType());

        return messageJSON;
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("jsontype must be JSONObject");
        JSONObject messageJSON = (JSONObject) jsonType;

        messageJSON.checkValidity(new String[] {"type", "msgID", "expiration", "chks", "message"});

        //get enum from int value
        type = TYPE.fromValue(messageJSON.getInt("type"));
        msgID = messageJSON.getInt("msgID");
        expiration = messageJSON.getLong("expiration"); //huh this method kinda usefull....
        //decode bytes in base64 strings
        chks = Base64.decode(messageJSON.getString("chks"));

        JSONObject messageObj = messageJSON.getObject("message");

        switch (type) {
            case TYPE.DATABASESTORE:
                message = new DatabaseStore(messageObj);
                break;
            case TYPE.DATABASELOOKUP:
                message = new DatabaseLookup(messageObj);
                break;
            case TYPE.DATABASESEARCHREPLY:
                message = new DatabaseSearchReply(messageObj);
                break;
            case TYPE.DELIVERYSTATUS:
                message = new DeliveryStatus(messageObj);
                break;
            default:
                throw new InvalidObjectException("Bad type: " + type);
        }
    }

    public TYPE getType() {
        return type;
    }

    public int getMsgID() {
        return msgID;
    }

    public Long getExpiration() {
        return expiration;
    }

    public I2NPMessage getMessage()  {
        return message;
    }
}
