package common.I2P.IDs;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
     * @param edSignPublicKey 32-byte Ed25519 public key for EdDSA signatures
     */
    Destination(PublicKey edSignPublicKey) {
        keys = new KeysAndCerts(null, edSignPublicKey);
    }

    /**
     * Creates Destination from JSON
     * @param json JSON
     * @throws InvalidObjectException throws if JSON is invalid
     */
    public Destination(JSONObject json) throws InvalidObjectException {
        keys = new KeysAndCerts(json);
    }

    public PublicKey getSigningPublicKey() {
        return keys.getSigningPublicKey();
    }

    /**
     * Get SHA256 byte hash of this destination
     * @return 32-byte SHA256 hash of this Destination
     */
    public byte[] getHash() {
        try {
            //hash payload of message
            MessageDigest md = MessageDigest.getInstance("SHA256");
            md.update(keys.getSigningPublicKey().getEncoded());

            //take only first 3 bytes of hash(I2P spec uses 1 but we are using base64 encoding is 6 bit aligned
            // So 3 bytes is the smallest we can have without padding bytes (24 bits divides evenly into 6)
            return md.digest();

        }
        catch (NoSuchAlgorithmException ex) {throw new RuntimeException(ex);} //should not hit this case
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
