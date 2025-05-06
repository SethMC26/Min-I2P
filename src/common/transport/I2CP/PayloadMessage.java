package common.transport.I2CP;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

import org.bouncycastle.util.encoders.Base64;

public class PayloadMessage extends I2CPMessage{

    private int messageID;
    private JSONObject payload;
    private byte[] encPayload;

    /**
     * Payload message sent from client to the router
     * @param sessionID Session ID of message
     * @param messageID ID of message
     * @param payload Payload of message
     */
    public PayloadMessage(int sessionID, int messageID, JSONObject payload) {
        super(sessionID, I2CPMessageTypes.PAYLOADMESSAGE);
        this.messageID = messageID;
        this.payload = payload;
    }

    /**
     * Payload message sent from router to the client
     * @param sessionID Session ID of message
     * @param messageID ID of message
     * @param encPayload Encrypted payload of message
     */
    public PayloadMessage(int sessionID, int messageID, byte[] encPayload) {
        super(sessionID, I2CPMessageTypes.PAYLOADMESSAGE);
        this.messageID = messageID;
        this.encPayload = encPayload;
    }
    

    public PayloadMessage(JSONObject json) throws InvalidObjectException {
        super(json);
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType); //super handles type check
        JSONObject json = (JSONObject) jsonType;

        messageID = json.getInt("messageID");
        if (json.containsKey("payload")) {
            payload = json.getObject("payload");
        } else if (json.containsKey("encPayload")) {
            encPayload = Base64.decode(json.getString("encPayload"));
        }
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = super.toJSONType();
        json.put("messageID", messageID);
        if (payload != null) {
            json.put("payload", payload);
        } else if (encPayload != null) {
            json.put("encPayload", Base64.toBase64String(encPayload));
        }
        return json;
    }

    public int getMessageID() {
        return messageID;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public byte[] getEncPayload() {
        return encPayload;
    }
}
