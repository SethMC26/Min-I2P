package common.I2P.I2NP;

import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;
import java.util.ArrayList;

/**
 * The response to a failed databaseLookup message
 */
public class DatabaseSearchReply extends I2NPMessage{
    /**
     * SHA256 hash of key
     */
    private byte[] key;
    /**
     * SHA256 hashes of RouterIdentity that router thinks is closest
     */
    private ArrayList<byte[]> peerHashes;
    /**
     * SHA256 of the RouterID of the router this reply was sent from
     */
    private byte[] fromHash;

    /**
     * Create DatabaseSearchReply message
     * @param key SHA256 hash key of object being searched
     * @param peerHashes ArrayList of SHA256 hash of RouterIDs that Router thinks is closest
     * @param fromHash SHA256 hash of RouterInfo of the Router this reply was sent from
     */
    public DatabaseSearchReply(byte[] key, ArrayList<byte[]> peerHashes, byte[] fromHash) {
        this.key = key;
        this.peerHashes = peerHashes;
        this.fromHash = fromHash;
    }

    /**
     * Create DatabaseSearchReply from json
     * @param json JSON to create message from
     * @throws InvalidObjectException throws if JSON is invalid
     */
    DatabaseSearchReply(JSONObject json) throws InvalidObjectException {
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("Must be JSONObject");

        JSONObject json = (JSONObject) jsonType;
        json.checkValidity(new String[] {"key", "from", "peerHashes"});

        key = Base64.decode(json.getString("key"));
        fromHash = Base64.decode(json.getString("from"));

        //get hashes from hashesArrau
        peerHashes = new ArrayList<>();
        JSONArray hashesArray = json.getArray("peerHashes");
        for (int i = 0; i < hashesArray.size(); i++) {
            peerHashes.add(Base64.decode(hashesArray.getString(i)));
        }
    }

    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", Base64.toBase64String(key));
        jsonObject.put("from", Base64.toBase64String(fromHash));

        //put all hashes in array list
        JSONArray hashesArray = new JSONArray();
        for (byte[] hash : peerHashes) {
            hashesArray.add(Base64.toBase64String(hash));
        }
        jsonObject.put("peerHashes", hashesArray);

        return jsonObject;
    }

    public byte[] getFromHash() {
        return fromHash;
    }

    public byte[] getKey() {
        return key;
    }

    public ArrayList<byte[]> getPeerHashes() {
        return peerHashes;
    }
}
