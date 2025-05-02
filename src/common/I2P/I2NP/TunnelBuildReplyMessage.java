package common.I2P.I2NP;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import common.I2P.I2NP.TunnelBuild.Record;
import common.I2P.IDs.RouterID;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

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

    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deserialize'");
    }

    @Override
    public JSONType toJSONType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'toJSONType'");
    }

    public int getTunnelID() {
        return tunnelID;
    }

    public boolean getIsSuccess() {
        return isSuccess;
    }
}
