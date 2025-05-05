package common.I2P.I2NP;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;

/**
 * Request to look something up in the network database
 *
 * @implNote did not include exlcuded peers in databaseLookup from spec and encrypted response since that
 * we are using ElGamal routers
 */
public class DatabaseLookup extends I2NPMessage{
    /**
     * SHA256 hash as the key to look up
     */
    private byte[] key;
    /**
     * SHA256 hash of RouterInfo who is asking or gateway to send reply to based on {@code replyFlag}
     */
    private byte[] fromHash;
    /**
     * Reply flag indicates how to send reply to this message {@code 0 => send reply directly} {@code 1 => send reply to some tunnel}
     */
    private int replyFlag;
    /**
     * Tunnel ID to send reply to
     * @apiNote only included if {@code deliveryFlag == 1}
     */
    private int replyTunnelID;

    /**
     * Create lookup message
     * @param key SHA256 hash - key of entry to lookup
     * @param fromHash SHA256 hash of RouterInfo request use {@code RouterInfo.getHash()}
     * @apiNote Must call {@code setReply(replyTunnelID)} if reply is needed
     */
    public DatabaseLookup(byte[] key, byte[] fromHash) {
        this.key = key;
        this.fromHash = fromHash;
        this.replyFlag = 0; //default sent reply directly
    }

    /**
     * Create DatabaseLookup from json
     * @param json JSON to deserialize
     * @throws InvalidObjectException throws if json is invalid
     */
    DatabaseLookup(JSONObject json) throws InvalidObjectException{
        deserialize(json);
    }

    /**
     * Set necessary data is reply is needed(in class sets reply token to 1)
     * @param replyTunnelID The tunnelID to send reply to
     */
    public void setReply(int replyTunnelID) {
        this.replyFlag = 1; //set reply flag to 1
        this.replyTunnelID = replyTunnelID;
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("Must be JSONObject");

        JSONObject json = (JSONObject) jsonType;
        json.checkValidity(new String[] {"key", "from", "replyFlag"});

        key = Base64.decode(json.getString("key"));
        fromHash = Base64.decode(json.getString("from"));
        replyFlag = json.getInt("replyFlag");

        //get reply data if necessary
        if (replyFlag == 1) {
            json.checkValidity(new String[] {"replyTunnelID"});
            replyTunnelID  = json.getInt("replyTunnelID");
        }
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("key", Base64.toBase64String(key));
        jsonObj.put("from", Base64.toBase64String(fromHash));
        jsonObj.put("replyFlag", replyFlag);

        //put reply data if necessary
        if (replyFlag == 1)
            jsonObj.put("replyTunnelID", replyTunnelID);

        return jsonObj;
    }

    public byte[] getKey() {
        return key;
    }

    public byte[] getFromHash() {
        return fromHash;
    }

    public int getReplyFlag() {
        return replyFlag;
    }

    public int getReplyTunnelID() {
        return replyTunnelID;
    }
}
