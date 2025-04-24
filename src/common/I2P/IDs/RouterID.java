package common.I2P.IDs;

import java.security.PublicKey;

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
}
