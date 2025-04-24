package common.I2P.IDs;

import java.security.Key;
import java.security.PublicKey;

/**
 * Class to uniquely identify destinations
 */
public class Destination {
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

    public PublicKey getElgamalPublicKey() {
        return keys.getSigningPublicKey();
    }

}
