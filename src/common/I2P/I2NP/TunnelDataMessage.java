package common.I2P.I2NP;

import java.io.InvalidObjectException;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class TunnelDataMessage extends I2NPMessage{
    private int tunnelID;
    private I2NPMessage payload;

    public TunnelDataMessage(int tunnelID, I2NPMessage payload) {
        this.tunnelID = tunnelID;
        // reminder to self, change this to byte[] when we have the payload
        // this is just a placeholder for now while testing tunnel builds
        this.payload = payload;
    }

    public TunnelDataMessage(JSONObject messageObj) {
        try {
            deserialize(messageObj);
        } catch (InvalidObjectException e) {
            e.printStackTrace();
        }
    }

    public int getTunnelID() {
        return tunnelID;
    }

    public I2NPMessage getPayload() {
        return payload;
    }

    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        if (!(arg0 instanceof JSONObject)) {
            throw new InvalidObjectException("Must be JSONObject");
        }

        JSONObject jsonObject = (JSONObject) arg0;
        this.tunnelID = jsonObject.getInt("tunnelID");
        this.payload = (I2NPMessage) jsonObject.get("payload"); // pray this works
    }

    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tunnelID", tunnelID);
        jsonObject.put("payload", payload);
        return jsonObject;
    }
}
