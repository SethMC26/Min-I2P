package common.message;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class Message implements JSONSerializable {

    protected String type;
    protected String destHash;

    /**
     * Constructor creates a new message object from JSONobject by deserializing it
     *
     * @param JSONMessage JSONObject representing the message
     */
    public Message(JSONObject JSONMessage) {
        try {
            deserialize(JSONMessage);
            // todo update try catch
        } catch (InvalidObjectException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Constructor creates a new message from parameters
     */
    public Message(String type) {
        this.type = type;
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("jsonType must be a JSONObject");
        JSONObject messageJSON = (JSONObject) jsonType;

        messageJSON.checkValidity(new String[] { "type", "hash" });

        type = messageJSON.getString("type");
        destHash = messageJSON.getString("hash");
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject messageJSON = new JSONObject();
        messageJSON.put("type", type);
        messageJSON.put("hash", destHash);
        return messageJSON;
    }

    /**
     * Get message type
     *
     * @return String of message type
     */
    public String getType() {
        return type;
    }

    /**
     * Get destination hash
     *
     * @return String of destination hash
     */
    public String getDestHash() {
        return destHash;
    }
}
