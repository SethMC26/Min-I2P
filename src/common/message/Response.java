package common.message;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class Response extends Message {

    private boolean status;
    private String payload;

    public Response(String type, boolean status, String payload) {
        super(type);
        this.status = status;
        this.payload = payload;
    }

    public Response(JSONObject JSONMessage) {
        super(JSONMessage);
        if (!super.type.equals("Status")) {
            throw new IllegalArgumentException("Bad type: " + super.type);
        }
    }

    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject)) {
            throw new InvalidObjectException("jsonType must be a JSONObject");
        }
        JSONObject messageJSON = (JSONObject) jsonType;

        messageJSON.checkValidity(new String[] { "type", "status", "payload" });
        type = messageJSON.getString("type");
        status = messageJSON.getBoolean("status");
        payload = messageJSON.getString("payload");
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject obj = super.toJSONType();

        obj.put("status", status);
        obj.put("payload", payload);

        return obj;
    }

    public boolean getStatus() {
        return status;
    }

    public String getPayload() {
        return payload;
    }
}
