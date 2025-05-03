package common.I2P.I2NP;

import common.I2P.IDs.RouterID;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class TunnelBuildReplyMessage extends I2NPMessage {

    /**
     * The router id of the tunnel gateway
     */
    private RouterID tunnelGateway;

    /**
     * Tunnel id of the tunnel
     */
    private int tunnelID;

    /**
     * Boolean value for successful creation or not
     */
    boolean isSuccess;

    /**
     * Reply message for tunnel build, all info to create lease
     * 
     * @param tunnelGateway
     * @param tunnelID
     * @param isSuccess
     */
    public TunnelBuildReplyMessage(int tunnelID, boolean isSuccess) {
        // this.tunnelGateway = tunnelGateway; // i think you can get this in the return router
        this.tunnelID = tunnelID;
        this.isSuccess = isSuccess;
    }

    public TunnelBuildReplyMessage(JSONObject messageObj) {
        try {
            deserialize(messageObj);
        } catch (InvalidObjectException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        if (!(arg0 instanceof JSONObject)) {
            throw new InvalidObjectException("Must be JSONObject");
        }

        JSONObject jsonObject = (JSONObject) arg0;
        this.tunnelGateway = new RouterID(jsonObject.getObject("tunnelGateway"));
        this.tunnelID = jsonObject.getInt("tunnelID");
        this.isSuccess = jsonObject.getBoolean("isSuccess");
    }

    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tunnelGateway", tunnelGateway.toJSONType()); // uhhhhhhh?
        jsonObject.put("tunnelID", tunnelID);
        jsonObject.put("isSuccess", isSuccess);
        return jsonObject;
    }

    public int getTunnelID() {
        return tunnelID;
    }

    public boolean getIsSuccess() {
        return isSuccess;
    }
}
