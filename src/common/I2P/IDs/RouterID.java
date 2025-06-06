package common.I2P.IDs;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

/**
 * Class to uniquely identify router
 */
public class RouterID implements JSONSerializable {
    /**
     * keys used - I2P spec uses KeysAndCerts for Destinations
     */
    private KeysAndCerts keys;

    /**
     * Create new RouterID with proper keys
     * @param elgamalPublicKey 512 byte elgamal public key
     * @param edSignPublicKey 32-byte Ed25519 public key for EdDSA signatures
     */
    public RouterID(PublicKey elgamalPublicKey, PublicKey edSignPublicKey) {
        keys = new KeysAndCerts(elgamalPublicKey, edSignPublicKey);
    }

    /**
     * Create new RouterID from JSON(wraps KeysAndCerts)
     * @param json JSONObject to deserialize
     * @throws throws if json is invalid
     */
    public RouterID(JSONObject json) throws InvalidObjectException{
        keys = new KeysAndCerts(json);
    }

    public PublicKey getElgamalPublicKey() {
        return keys.getPublicKey();
    }

    public PublicKey getSigningPublicKey() {
        return keys.getSigningPublicKey();
    }

    /**
     * Gets SHA-256 hash of this RouterID
     * @return 32 byte SHA256 hash of this RouterID
     */
    public byte[] getHash() {
        try {
            //hash payload of message
            MessageDigest md = MessageDigest.getInstance("SHA256");
            md.update(keys.getPublicKey().getEncoded());
            md.update(keys.getSigningPublicKey().getEncoded());

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
