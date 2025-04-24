package common.I2P.IDs;

import java.security.PublicKey;

public class KeysAndCerts {
    /**
     * Elgamal 256-byte public key
     */
    private PublicKey publicKey;
    /**
     * DSA_SHA1 128-byte key for verification
     */
    private PublicKey signingPublicKey;

    /**
     * Create KeysAndCerts class with a elgamal public key and a signing public key
     * @param publicKey Elgamal 256-byte public key
     * @param signingPublicKey DSA_SHA1 128 byte key for verifying signatures
     *
     * @implSpec Spec includes a Certificate but for our implementations, we will be using a deprecated version without
     * proof of work mechanisms for key certificates. I think we should be be able to get away with this.
     */
    KeysAndCerts(PublicKey publicKey, PublicKey signingPublicKey) {
        this.publicKey = publicKey;
        this.signingPublicKey = signingPublicKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PublicKey getSigningPublicKey() {
        return signingPublicKey;
    }
}
