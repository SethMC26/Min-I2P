package common.I2P.NetworkDB;

import common.I2P.IDs.Destination;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import java.security.*;
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
     * Signature of this LeaseSet by Destinations Signing Private key
     */
    private byte[] signature;
    /**
     * Leases in lease set
     */
    HashSet<Lease> leases;
    // seth told me this is really efficient which is really cool - sam

    /**
     * Create new LeaseSet
     * @param leases HashSet containing leases
     * @param destination destination lease belong to
     * @param encryptionKey elgamal public key for encryption
     * @param signingKey PrivateKey to use to sign this LeaseSet - should be signed by destination
     */
    public LeaseSet(HashSet<Lease> leases, Destination destination, PublicKey encryptionKey, PrivateKey signingKey) {
        super(RecordType.LEASESET);
        this.leases = leases;
        this.destination = destination;
        this.encryptionKey = encryptionKey;

        //sign LeaseSet
        try {
            //get signature ready
            Signature signing = Signature.getInstance("Ed25519");
            signing.initSign(signingKey);
            //sign this leases
            for (Lease lease : leases) {
                signing.update(lease.getTunnelGW());
                signing.update(ByteBuffer.allocate(Integer.BYTES).putInt(lease.getTunnelID()).array());
                signing.update(ByteBuffer.allocate(Long.BYTES).putLong(lease.getExpiration()).array());
            }
            //sign destination
            signing.update(destination.getSigningPublicKey().getEncoded());
            //sign encryption key
            signing.update(encryptionKey.getEncoded());
            //get signature
            this.signature = signing.sign();
        } catch (NoSuchAlgorithmException | SignatureException e) {
            throw new RuntimeException(e); //should never hit case
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Bad private key for SHA1withDSA" + e);
        }
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

    /**
     * Verify the signature of this record given a public key
     * @return true if signature is valid false otherwise
     */
    @Override
    public boolean verifySignature() {
        //sign LeaseSet
        try {
            //get signature ready
            Signature signing = Signature.getInstance("Ed25519");
            signing.initVerify(destination.getSigningPublicKey());
            //sign this leases
            for (Lease lease : leases) {
                signing.update(lease.getTunnelGW());
                signing.update(ByteBuffer.allocate(Integer.BYTES).putInt(lease.getTunnelID()).array());
                signing.update(ByteBuffer.allocate(Long.BYTES).putLong(lease.getExpiration()).array());
            }
            //sign destination
            signing.update(destination.getSigningPublicKey().getEncoded());
            //sign encryption key
            signing.update(encryptionKey.getEncoded());
            //verify signature
            return signing.verify(signature);
        } catch (NoSuchAlgorithmException | SignatureException e) {
            throw new RuntimeException(e); //should never hit case
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Bad private key for SHA1withDSA" + e);
        }
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("Must be JSONObject");

        JSONObject json = (JSONObject) jsonType;
        json.checkValidity(new String[] {"destination", "encryptionKey", "signature", "leases"});

        destination = new Destination(json.getObject("destination"));
        signature = Base64.decode(json.getString("signature"));

        //lets decode the encryption key from the bytes
        byte[] publicKeyBytes = Base64.decode(json.getString("encryptionKey"));
        try {
            encryptionKey = KeyFactory.getInstance("ElGamal").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        }
        catch (InvalidKeySpecException e) {throw new InvalidObjectException("Public Key is not valid");}
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
        json.put("signature", Base64.toBase64String(signature));

        //add leases as an array
        JSONArray leasesArray = new JSONArray();
        for(Lease lease : leases) {
            leasesArray.add(lease.toJSONType());
        }
        json.put("leases", leasesArray);

        return json;
    }

    public HashSet<Lease> getLeases() {
        return leases;
    }

    public Destination getDestination() {
        return destination;
    }

    public PublicKey getEncryptionKey() {
        return encryptionKey;
    }
}
