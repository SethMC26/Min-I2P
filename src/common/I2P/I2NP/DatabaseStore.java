package common.I2P.I2NP;

import common.I2P.NetworkDB.Lease;
import common.I2P.NetworkDB.LeaseSet;
import common.I2P.NetworkDB.Record;
import common.I2P.NetworkDB.RouterInfo;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;

public class DatabaseStore extends I2NPMessage{
    /**
     * SHA256 hash as key
     */
    private byte[] key;

    /**
     * Type of storage {@code 0 for RouterInfo, 1 for LeaseSet}
     */
    private int storeType;

    /**
     * Reply Token if greater, a deliveryStatusMessage is request(reliable mode), floodfill router is also expected to pass
     * data to peers if greater than zero
     */
    private int replyToken = 0;

    /**
     * TunnelID of the inbound gateway the response should be sent to
     * @apiNote only needed if {@code replyToken}  > 0
     */
    private int replyTunnelID;

    /**
     * SHA256 hash of routerInfo of the routerInfo entry to reach the gateway
     * @apiNote only needed if {@code replyToken}  > 0
     * @apiNote if {@code replyToken} = 0 then this is the router hash the response should be sent to
     */
    private byte[] replyGateway;

    /**
     * Record to store {@code must be RouterInfo or LeaseSet}
     */
    private Record record;

    /**
     * Create I2NPMessage from JSONObject
     *
     * @param message@throws InvalidObjectException Throws if JSONObject is not valid for type of I2NPMessage
     */
    DatabaseStore(JSONObject message) throws InvalidObjectException {
        deserialize(message);
    }

    /**
     * Create DatabaseStore message for {@code record}
     * @param record Record to store
     */
    public DatabaseStore(Record record) {
        switch(record.getRecordType()) {
            case ROUTERINFO -> this.storeType = 0;
            case LEASESET -> this.storeType = 1;
            default -> throw new IllegalArgumentException("bad type " + record.getRecordType()); //should never hit case
        }
        this.key = record.getHash();
        this.record = record;
    }

    /**
     * Set necessary data is reply is needed(in class sets reply token to 1)
     * @param replyTunnelID The tunnelID of the inbound gateway of the tunnel the reponse should be sent to
     * @param replyGateway SHA256 hash of the RouterInfo entry to reach the gateway - use {@code RouterInfo.getHash()}.
     */
    public void setReply(int replyTunnelID, byte[] replyGateway) {
        this.replyToken = 1;
        this.replyTunnelID = replyTunnelID;
        this.replyGateway = replyGateway;
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("Must be JSONObject");

        JSONObject json = (JSONObject) jsonType;
        json.checkValidity(new String[] {"type", "key", "replyToken", "data"});

        storeType = json.getInt("type");
        key = Base64.decode(json.getString("key"));
        replyToken = json.getInt("replyToken");

        //if reply token greater than 0 then we need to get reply data
        if (replyToken > 0) {
            json.checkValidity(new String[]{"replyTunnelID", "replyGateway"});
            replyTunnelID = json.getInt("replyTunnelID");
            replyGateway = Base64.decode(json.getString("replyGateway"));

        }

        //switch based on type of database store to get proper date out
        switch(storeType) {
            case 0-> record = new RouterInfo(json.getObject("data"));
            case 1 -> record = new LeaseSet(json.getObject("data"));
            default -> throw new InvalidObjectException("Bad store type " + storeType);
        }

    }

    @Override
    public JSONObject toJSONType() {
        JSONObject databaseStoreJSON = new JSONObject();

        databaseStoreJSON.put("type", storeType);
        databaseStoreJSON.put("key", Base64.toBase64String(key));
        databaseStoreJSON.put("replyToken", replyToken);

        //if reply token greater than 0 then we need reply data
        if (replyToken > 0 ) {
            databaseStoreJSON.put("replyTunnelID", replyTunnelID);
            databaseStoreJSON.put("replyGateway", Base64.toBase64String(replyGateway));
        }

        //add record
        databaseStoreJSON.put("data", record.toJSONType());

        return databaseStoreJSON;
    }

    public int getStoreType() {
        return storeType;
    }

    public Record getRecord() {
        return record;
    }

    public RouterInfo getRouterInfo() {
        if (storeType != 0)
            throw new IllegalArgumentException("Type must be 1 to get RouterInfo Type: " + storeType);
        return (RouterInfo) record;
    }

    public LeaseSet getLeaseSet() {
        if (storeType != 1)
            throw new IllegalArgumentException("Type must be 0 to get leaseSet Type: " +storeType);
        return (LeaseSet) record;
    }

    public int getReplyToken() {
        return replyToken;
    }

    public int getReplyTunnelID() {
        return replyTunnelID;
    }

    public byte[] getReplyGateway() {
        return replyGateway;
    }

    public byte[] getKey() {
        return key;
    }
}
