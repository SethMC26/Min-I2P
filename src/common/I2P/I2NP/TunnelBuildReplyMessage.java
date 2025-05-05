package common.I2P.I2NP;

import common.I2P.IDs.RouterID;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class TunnelBuildReplyMessage extends I2NPMessage {

    /**
     * The router id of the tunnel gateway
     */
    private int nextTunnel;

    /**
     * Tunnel id of a node in the originating tunnel
     * (the one that sent the message to the tunnel gateway)
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
    public TunnelBuildReplyMessage(int nextTunnel, int tunnelID, boolean isSuccess) {
        // this.tunnelGateway = tunnelGateway; // i think you can get this in the return router
        this.nextTunnel = nextTunnel;
        this.tunnelID = tunnelID;
        this.isSuccess = isSuccess;
    }

    public TunnelBuildReplyMessage(JSONObject messageObj) throws InvalidObjectException {
        deserialize(messageObj);
    }

    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        if (!(arg0 instanceof JSONObject)) {
            throw new InvalidObjectException("Must be JSONObject");
        }

        JSONObject jsonObject = (JSONObject) arg0;
        jsonObject.checkValidity(new String[] {"nextTunnel", "tunnelID", "isSuccess"});
        //im commenting this out to avoid serialization error - seth
        //this.tunnelGateway = new RouterID(jsonObject.getObject("tunnelGateway"));
        this.nextTunnel = jsonObject.getInt("nextTunnel");
        this.tunnelID = jsonObject.getInt("tunnelID");
        this.isSuccess = jsonObject.getBoolean("isSuccess");
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject jsonObject = new JSONObject();
        //commenting out to avoid serialization error -seth
        //jsonObject.put("tunnelGateway", tunnelGateway.toJSONType()); // uhhhhhhh?
        jsonObject.put("nextTunnel", nextTunnel);
        jsonObject.put("tunnelID", tunnelID);
        jsonObject.put("isSuccess", isSuccess);
        return jsonObject;
    }

    public int getNextTunnel() {
        return nextTunnel;
    }

    public int getTunnelID() {
        return tunnelID;
    }

    public boolean getIsSuccess() {
        return isSuccess;
    }

    public void setNextTunnel(int nextTunnel) {
        this.nextTunnel = nextTunnel;
    }
}
