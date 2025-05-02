package common.transport.I2CP;

import common.I2P.NetworkDB.LeaseSet;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class CreateLeaseSet extends I2CPMessage {
    /**
     * Private key corresponding to public key in lease set
     */
    private PrivateKey privateKey;
    /**
     * Lease Set for use by the router
     */
    private LeaseSet leaseSet;

    /**
     * Message sent from Client to Router to create a lease set for the router to use
     * @param sessionID ID of session
     * @param privateKey Private key signing key for ED25519 corresponding to public key in Lease Set
     * @param leaseSet Lease set for use by the Router
     */
    public CreateLeaseSet(int sessionID, PrivateKey privateKey, LeaseSet leaseSet) {
        super(sessionID, I2CPMessageTypes.CREATELEASESET);
        this.privateKey = privateKey;
        this.leaseSet = leaseSet;
    }

    public CreateLeaseSet(JSONObject json) throws InvalidObjectException {
        super(json);
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType); //super handle type check
        JSONObject json = (JSONObject) jsonType;

        json.checkValidity(new String[]{"privateKey", "leaseSet"});
        leaseSet = new LeaseSet(json.getObject("leaseSet"));


        //generate key from bytes
        byte[] privKeyBytes = Base64.decode(json.getString("privKey"));
        try {
            privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(
                    new PKCS8EncodedKeySpec(privKeyBytes));
        }
        catch (InvalidKeySpecException e) {throw new InvalidObjectException("private Key is not valid");}
        catch (NoSuchAlgorithmException e) {throw new RuntimeException(e);} //should never hit case
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = super.toJSONType();
        json.put("privKey", Base64.toBase64String(privateKey.getEncoded()));
        json.put("leaseSet", leaseSet.toJSONType());
        return json;
    }
}
