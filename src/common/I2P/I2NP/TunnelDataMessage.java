package common.I2P.I2NP;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

import org.bouncycastle.util.encoders.Base64;

public class TunnelDataMessage extends I2NPMessage {
    private int tunnelID;
    private JSONObject payload;
    private byte[] encPayload;

    public TunnelDataMessage(int tunnelID, JSONObject payload) {
        this.tunnelID = tunnelID;
        // reminder to self, change this to byte[] when we have the payload
        // this is just a placeholder for now while testing tunnel builds
        this.payload = payload;
    }

    public TunnelDataMessage(int tunnelID, byte[] payload) {
        this.tunnelID = tunnelID;
        // reminder to self, change this to byte[] when we have the payload
        // this is just a placeholder for now while testing tunnel builds
        this.encPayload = payload;
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

    public byte[] getEncPayload() {
        return encPayload;
    }

    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        if (!(arg0 instanceof JSONObject)) {
            throw new InvalidObjectException("Must be JSONObject");
        }
        JSONObject jsonObject = (JSONObject) arg0;
        jsonObject.checkValidity(new String[] { "tunnelID" });
        this.tunnelID = jsonObject.getInt("tunnelID");
        if (jsonObject.containsKey("payload")) {
            this.payload = jsonObject.getObject("payload");
            this.payload = castDoublesToIntegers(this.payload); // fix any Double values to Integer
        } else if (jsonObject.containsKey("encPayload")) {
            this.encPayload = Base64.decode(jsonObject.getString("encPayload"));
        } else {
            throw new InvalidObjectException("Must contain payload or encPayload");
        }
        // Cast any Double values in the payload to Integer
        System.out.println("TunnelDataMessage deserializes to: " + tunnelID + " " + payload);
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tunnelID", tunnelID);
        if (encPayload != null) {
            jsonObject.put("encPayload", Base64.toBase64String(encPayload));
        } else if (payload != null) {
            jsonObject.put("payload", payload);
        }

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
