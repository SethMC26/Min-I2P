package common.I2P.IDs;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;
import java.security.Key;
import java.security.PublicKey;

/**
 * Class to uniquely identify destinations
 */
public class Destination implements JSONSerializable {
    /**
     * keys used - I2P spec uses KeysAndCerts for Destinations
     */
    private KeysAndCerts keys;

    /**
     * Create destination with DSA_SHA1_Public_key
     * @param DSASHA1PublicKey 128 byte DSA_SHA1_Public_Key
     */
    Destination(PublicKey DSASHA1PublicKey) {
        keys = new KeysAndCerts(null, DSASHA1PublicKey);
    }

    /**
     * Creates Destination from JSON
     * @param json JSON
     * @throws InvalidObjectException throws if JSON is invalid
     */
    public Destination(JSONObject json) throws InvalidObjectException {
        keys = new KeysAndCerts(json);
    }

    public PublicKey getElgamalPublicKey() {
        return keys.getSigningPublicKey();
    }

    /**
     * wraps KeysAndCerts deserialize
     */
    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("jsontype must be a JSONObject");
        keys = new KeysAndCerts((JSONObject) jsonType);
    }

    /**
     * Wraps KeysAndCerts toJSONType
     */
    @Override
    public JSONObject toJSONType() {
        return keys.toJSONType();
    }
}
