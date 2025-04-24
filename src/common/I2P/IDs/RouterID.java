package common.I2P.IDs;

import org.bouncycastle.util.encoders.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;

/**
 * Class to uniquely identify router
 */
public class RouterID {
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

    public PublicKey getElgamalPublicKey() {
        return keys.getPublicKey();
    }

    public PublicKey getDSASHA1PublicKey() {
        return keys.getSigningPublicKey();
    }


    /**
     * Gets first 16 bytes of SHA-256 hash of this RouterID
     * @return Base64 encoding of the first 16 bytes of RouterID SHA256 hash
     */
    public String getHash() {
        try {
            //hash payload of message
            MessageDigest md = MessageDigest.getInstance("SHA256");
            md.update(keys.getPublicKey().getEncoded());
            md.update(keys.getSigningPublicKey().getEncoded());

            //take only first 3 bytes of hash(I2P spec uses 1 but we are using base64 encoding is 6 bit aligned
            // So 3 bytes is the smallest we can have without padding bytes (24 bits divides evenly into 6)
            return Base64.toBase64String(Arrays.copyOfRange(md.digest(), 0, 16));

        }
        catch (NoSuchAlgorithmException ex) {throw new RuntimeException(ex);} //should not hit this case
    }
}
