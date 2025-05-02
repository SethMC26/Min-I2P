package common.I2P.I2NP;

import java.io.InvalidObjectException;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class TunnelDataMessage extends I2NPMessage{
    private int tunnelID;
    private byte[] payload;

    public TunnelDataMessage(int tunnelID, byte[] payload) {
        this.tunnelID = tunnelID;
        this.payload = payload;
    }

    public int getTunnelID() {
        return tunnelID;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        if (!(arg0 instanceof JSONObject)) {
            throw new InvalidObjectException("Must be JSONObject");
        }

        JSONObject jsonObject = (JSONObject) arg0;
        this.tunnelID = jsonObject.getInt("tunnelID");
        this.payload = jsonObject.getString("payload").getBytes(); // pray this works
    }

    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tunnelID", tunnelID);
        jsonObject.put("payload", payload);
        return jsonObject;
    }
}
