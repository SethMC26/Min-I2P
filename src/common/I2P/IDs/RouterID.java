package common.I2P.IDs;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;

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
     * @param elgamalPublicKey 256 byte elgamal public key
     * @param DSASHA1PublicKey 128 byte DSA-SHA1 public key
     */
    RouterID(PublicKey elgamalPublicKey, PublicKey DSASHA1PublicKey) {
        keys = new KeysAndCerts(elgamalPublicKey, DSASHA1PublicKey);
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

    public PublicKey getDSASHA1PublicKey() {
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
