package common.I2P.I2NP;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class TunnelDataMessage extends I2NPMessage {
    private int tunnelID;
    private JSONObject payload;

    public TunnelDataMessage(int tunnelID, JSONObject payload) {
        this.tunnelID = tunnelID;
        // reminder to self, change this to byte[] when we have the payload
        // this is just a placeholder for now while testing tunnel builds
        this.payload = payload;
    }

    public TunnelDataMessage(JSONObject messageObj) throws InvalidObjectException {
        deserialize(messageObj);
    }

    public int getTunnelID() {
        return tunnelID;
    }

    public void setTunnelID(int tunnelID) {
        this.tunnelID = tunnelID;
    }

    public JSONObject getPayload() {
        return payload;
    }

    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        if (!(arg0 instanceof JSONObject)) {
            throw new InvalidObjectException("Must be JSONObject");
        }
        JSONObject jsonObject = (JSONObject) arg0;
        jsonObject.checkValidity(new String[] { "tunnelID", "payload" });
        this.tunnelID = jsonObject.getInt("tunnelID");
        this.payload = jsonObject.getObject("payload"); // pray this works;
        // Cast any Double values in the payload to Integer
        this.payload = castDoublesToIntegers(this.payload);
        System.out.println("TunnelDataMessage deserializes to: " + tunnelID + " " + payload);
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tunnelID", tunnelID);
        jsonObject.put("payload", payload);

        return jsonObject;
    }

    private JSONObject castDoublesToIntegers(JSONObject jsonObject) {
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            if (value instanceof Double) {
                // Cast Double to Integer
                jsonObject.put(key, ((Double) value).intValue());
            } else if (value instanceof JSONObject) {
                // Recursively process nested JSONObjects`
                castDoublesToIntegers((JSONObject) value);
            }
        }
        return jsonObject;
    }
}
