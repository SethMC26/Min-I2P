package common.I2P.NetworkDB;

import common.I2P.IDs.RouterID;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;

public class Lease implements JSONSerializable {
    /**
     * Hash of RouterID of the tunnel Gateway
     */
    private byte[] tunnelGW;

    /**
     * Tunnel ID for lease
     */
    private int tunnelID;

    /**
     * Epoch time of expiration
     */
    private long expiration;

    /**
     * Creates Lease
     * @param tunnelRouter RouterID of tunnel gateway
     * @param tunnelID tunnel ID for lease
     * @param expiration Expiration of lease in epoch time
     */
    public Lease(RouterID tunnelRouter, int tunnelID) {
        this.tunnelGW = tunnelRouter.getHash();
        this.tunnelID = tunnelID;
        // this.expiration = expiration; no longer needed expiration is no longer used
    }

    /**
     * Creates lease from JSONObject
     * @param json JSON to deserialize
     * @throws throws if json is invalid
     */
    public Lease(JSONObject json) throws InvalidObjectException {
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("Must be JSONObject");

        JSONObject json = (JSONObject) jsonType;
        json.checkValidity(new String[] {"tunnelGW", "tunnelID", "expiration"});
        tunnelGW = Base64.decode(json.getString("tunnelGW"));
        tunnelID = json.getInt("tunnelID");
        // expiration = json.getLong("expiration"); //well ill be darned ant that handy
        // stroke your damn ego a little more seth - from sam
    }


    @Override
    public JSONObject toJSONType() {
        JSONObject json = new JSONObject();

        json.put("tunnelGW", Base64.toBase64String(tunnelGW));
        json.put("tunnelID", tunnelID);
        json.put("expiration", expiration);

        return json;
    }

    public byte[] getTunnelGW() {
        return tunnelGW;
    }

    public int getTunnelID() {
        return tunnelID;
    }

    public long getExpiration() {
        return expiration;
    }
}
