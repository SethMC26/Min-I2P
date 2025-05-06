package common.I2P.I2NP;

import common.I2P.IDs.RouterID;
import common.I2P.tunnels.TunnelObject;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class TunnelBuildReplyMessage extends I2NPMessage {

    /**
     * The router id of the tunnel gateway
     */
    private int nextTunnel;

    /**
     * Tunnel id of the endpoint of the tunnel build request
     */
    private int tunnelID; // leaving this in so i dont have to change any logic in the code hehe


    /**
     * The tunnel id of the tunnel build request
     */
    private TunnelBuild records;

    /**
     * Reply message for tunnel build, all info to create lease
     * 
     * @param tunnelGateway
     * @param tunnelID
     * @param records
     */
    public TunnelBuildReplyMessage(int nextTunnel, int tunnelID, TunnelBuild records) {
        // this.tunnelGateway = tunnelGateway; // i think you can get this in the return router
        this.nextTunnel = nextTunnel;
        this.tunnelID = tunnelID;
        this.records = records;
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
        jsonObject.checkValidity(new String[] {"nextTunnel", "tunnelID", "records"});

        this.nextTunnel = jsonObject.getInt("nextTunnel");
        this.tunnelID = jsonObject.getInt("tunnelID");
        this.records = new TunnelBuild(jsonObject.getArray("records"));
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nextTunnel", nextTunnel);
        jsonObject.put("tunnelID", tunnelID);
        jsonObject.put("records", records.toJSONType());
        // System.out.println("TunnelBuildReplyMessage serializes to: " + jsonObject.getFormattedJSON());
        return jsonObject;
    }

    public int getNextTunnel() {
        return nextTunnel;
    }

    public int getTunnelID() {
        return tunnelID;
    }

    public TunnelBuild getRecords() {
        return records;
    }

    public void setNextTunnel(int nextTunnel) {
        this.nextTunnel = nextTunnel;
    }
}
