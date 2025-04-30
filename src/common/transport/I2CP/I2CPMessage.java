package common.transport.I2CP;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;
import java.security.SecureRandom;

public class I2CPMessage implements JSONSerializable {
    private int messageID;
    private int sessionID;
    private I2CPMessageTypes type;

    I2CPMessage(int messageID, int sessionID, I2CPMessageTypes type) {
        this.messageID = messageID;
        this.sessionID = sessionID;
        this.type = type;
    }

    I2CPMessage(JSONObject json) throws InvalidObjectException {
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof  JSONObject))
            throw new InvalidObjectException("Must be JSONObject");

        JSONObject json = (JSONObject) jsonType;
        json.checkValidity(new String[] {"messageID", "sessionID", "type"});

        messageID = json.getInt("messageID");
        sessionID = json.getInt("sessionID");
        type = I2CPMessageTypes.values()[json.getInt("type")]; //get enum from ordinal using array of values
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = new JSONObject();
        json.put("messageID", messageID);
        json.put("sessionID", sessionID);
        json.put("type", type.ordinal());
        return json;
    }

    public I2CPMessageTypes getType() {
        return type;
    }

    public int getMessageID() {
        return messageID;
    }

    public int getSessionID() {
        return sessionID;
    }
}
