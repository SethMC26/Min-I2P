package common.I2P.IDs;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class KeysAndCerts implements JSONSerializable {
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

    /**
     * Create KeysAndCerts class from JSON
     * @param json json to create class from
     * @throws InvalidObjectException throws if JSON is invalid
     */
    KeysAndCerts(JSONObject json) throws InvalidObjectException{
        deserialize(json);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PublicKey getSigningPublicKey() {
        return signingPublicKey;
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("Must be JSONObject");

        JSONObject keysAndCertsJSON = (JSONObject) jsonType;
        if (!keysAndCertsJSON.containsKey("publicKey"))
            throw new InvalidObjectException("Missing key - publicKey");

        byte[] publicKeyBytes = Base64.decode(keysAndCertsJSON.getString("publickey"));
        try {
            publicKey = KeyFactory.getInstance("ElGamal").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        }
        catch (InvalidKeySpecException e) {throw new InvalidObjectException("Public Key is not valid");}
        catch (NoSuchAlgorithmException e) {throw new RuntimeException(e);} //should never hit case

        //this could be null for destination
        if (keysAndCertsJSON.containsKey("signingPublicKey")) {
            byte[] signingKeyBytes = Base64.decode(keysAndCertsJSON.getString("signingPublicKey"));
            try {
                signingPublicKey = KeyFactory.getInstance("DSA").generatePublic(
                        new X509EncodedKeySpec(signingKeyBytes));
            }
            catch (InvalidKeySpecException e) {throw new InvalidObjectException("Signing Key is not valid");}
            catch (NoSuchAlgorithmException e) {throw new RuntimeException(e);} //should never hit case
        }
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = new JSONObject();
        json.put("publicKey", Base64.toBase64String(publicKey.getEncoded()));

        //could be null if used for destination
        if (signingPublicKey != null) {
            json.put("signingPublicKey", Base64.toBase64String(signingPublicKey.getEncoded()));
        }

        return json;
    }
}
