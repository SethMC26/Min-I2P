package common.I2P.NetworkDB;

import common.I2P.IDs.Destination;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;

public class LeaseSet extends Record implements JSONSerializable {
    /**
     * Destination leases belong to
     */
    private Destination destination;
    /**
     * Public key for which garlic messages can be encrypted to
     */
    private PublicKey encryptionKey;
    /**
     * Signing public key that can be used to revoke leaseSet
     * @implNote Spec includes this but im not sure if we need it
     */
    private PublicKey signingPublicKey;
    /**
     * Signature of this LeaseSet by Destinations Signing Private key
     */
    private byte[] signature;
    /**
     * Leases in lease set
     */
    HashSet<Lease> leases;

    /**
     * Create new LeaseSet
     * @param leases HashSet containing leases
     * @param destination destination lease belong to
     * @param encryptionKey elgamal public key for encryption
     * @param singingPublicKey signingPublicKey key to verify signature
     * @param signature Signature of all data using the corresponding private signing key to {@code signingPublicKey}
     */
    public LeaseSet(HashSet<Lease> leases, Destination destination, PublicKey encryptionKey, PublicKey singingPublicKey, byte[] signature) {
        super(RecordType.LEASESET);
        this.leases = leases;
        this.destination = destination;
        this.encryptionKey = encryptionKey;
        this.signingPublicKey = singingPublicKey;
        this.signature = signature;
    }

    /**
     * Create LeaseSet from JSON
     * @param json JSONObject to deserialize
     * @throws InvalidObjectException throws if JSON is invalid
     */
    public LeaseSet(JSONObject json) throws InvalidObjectException{
        super(RecordType.LEASESET);
        deserialize(json);
    }

    /**
     * Get SHA256 hash of destination
     * @return 32 byte sha256 hash
     */
    @Override
    public byte[] getHash() {
       return destination.getHash();
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("Must be JSONObject");

        JSONObject json = (JSONObject) jsonType;
        json.checkValidity(new String[] {"destination", "encryptionKey", "signingPublicKey", "signature", "leases"});

        destination = new Destination(json.getObject("destination"));
        signature = Base64.decode(json.getString("signature"));

        //lets decode the encryption key from the bytes
        byte[] publicKeyBytes = Base64.decode(json.getString("encryptionKey"));
        try {
            encryptionKey = KeyFactory.getInstance("ElGamal").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        }
        catch (InvalidKeySpecException e) {throw new InvalidObjectException("Public Key is not valid");}
        catch (NoSuchAlgorithmException e) {throw new RuntimeException(e);} //should never hit case

        //lets decode the signature from the bytes
        byte[] signingKeyBytes = Base64.decode(json.getString("signingPublicKey"));
        try {
            signingPublicKey = KeyFactory.getInstance("DSA").generatePublic(
                    new X509EncodedKeySpec(signingKeyBytes));
        }
        catch (InvalidKeySpecException e) {throw new InvalidObjectException("Signing Key is not valid");}
        catch (NoSuchAlgorithmException e) {throw new RuntimeException(e);} //should never hit case

        //add all Leases in under "leases"
        JSONArray leasesArray = json.getArray("leases");
        leases = new HashSet<>();
        for (int i = 0; i < leasesArray.size(); i++ ) {
            leases.add(new Lease(leasesArray.getObject(i)));
        }

        //todo check to make sure signature is valid
    }

    /**
     * @return
     */
    @Override
    public JSONObject toJSONType() {
        JSONObject json = new JSONObject();
        json.put("destination", destination.toJSONType());
        json.put("encryptionKey", Base64.toBase64String(encryptionKey.getEncoded()));
        json.put("signingPublicKey", Base64.toBase64String(signingPublicKey.getEncoded()));
        json.put("signature", Base64.toBase64String(signature));

        //add leases as an array
        JSONArray leasesArray = new JSONArray();
        for(Lease lease : leases) {
            leasesArray.add(lease.toJSONType());
        }
        json.put("leases", leasesArray);

        return json;
    }
}
