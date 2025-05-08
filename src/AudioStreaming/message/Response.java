package AudioStreaming.message;

import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class Response extends Message {

    // -------- Private Variables -------- //
    private boolean status;
    private String payload;

    /**
     * This is the constuctor for the AudioStreaming.server sending a response to the user
     *
     * @param type - String type of message
     * @param status - boolean status of the message
     * @param payload - String payload of the message
     */
    public Response(String type, String desthash, boolean status, String payload) {
        super(type, desthash);
        this.status = status;
        this.payload = payload;
    }

    /**
     * This is the constructor for the AudioStreaming.server sending a response to the user
     *
     * @param JSONMessage - JSONObject to deserialize
     */
    public Response(JSONObject JSONMessage) {
        super(JSONMessage);
        if (!super.type.equals("Status")) {
            throw new IllegalArgumentException("Bad type: " + super.type);
        }
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType);

        JSONObject messageJSON = (JSONObject) jsonType;

        messageJSON.checkValidity(new String[] { "status", "payload" });

        status = messageJSON.getBoolean("status");
        payload = messageJSON.getString("payload");
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject obj = super.toJSONType();

        if (!super.type.equals("List")) {
            obj.put("status", status);
            obj.put("payload", payload);
        } else {
            obj.put("status", status);
            JSONArray array = new JSONArray();
            for (String key : payload.split(",")) {
                JSONObject audio = new JSONObject();
                audio.put("audioName", key);
                array.add(audio);
            }
            obj.put("payload", array);
        }

        return obj;
    }

    public boolean getStatus() {
        return status;
    }

    public String getPayload() {
        return payload;
    }
}
