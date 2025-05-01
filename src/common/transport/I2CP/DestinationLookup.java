package common.transport.I2CP;

import common.I2P.IDs.Destination;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;

import static common.transport.I2CP.I2CPMessageTypes.DESTLOOKUP;

/**
 * Destination lookup message
 * @apiNote Can be done within a standard session or stand alone
 */
public class DestinationLookup extends I2CPMessage{
    /**
     * SHA256 hash of destination
     */
    private byte[] hash;

    /**
     * Festination lookup message in a session
     * @param sessionID ID of session
     * @param hash SHA256 hash of destination
     * @apiNote this is used when in session
     */
    public DestinationLookup(int sessionID, byte[] hash) {
        super(sessionID, DESTLOOKUP);
        this.hash = hash;
    }

    /**
     * Destination lookup message outside of a standard session
     * @param hash SHA256 hash of destination
     * @apiNote This is used when NOT in a session
     */
    public DestinationLookup(byte[] hash) {
        super(0, DESTLOOKUP);
        this.hash = hash;
    }

    DestinationLookup(JSONObject json) throws InvalidObjectException {
        super(json);
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType);
        JSONObject json = (JSONObject) jsonType;
        json.checkValidity(new String[] {"hash"});

        this.hash = Base64.decode(json.getString("hash"));
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = super.toJSONType();
        json.put("hash", Base64.toBase64String(hash));
        return json;
    }

    public byte[] getHash() {
        return hash;
    }
}
