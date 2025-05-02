package common.transport.I2CP;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class PayloadMessage extends I2CPMessage{

    private int messageID;
    private JSONObject payload;

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

    public PayloadMessage(JSONObject json) throws InvalidObjectException {
        super(json);
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType); //super handles type check
        JSONObject json = (JSONObject) jsonType;

        messageID = json.getInt("messageID");
        payload = json.getObject("payload");
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = super.toJSONType();
        json.put("messageID", messageID);
        json.put("payload", payload);
        return json;
    }

    public int getMessageID() {
        return messageID;
    }

    public JSONObject getPayload() {
        return payload;
    }
}
