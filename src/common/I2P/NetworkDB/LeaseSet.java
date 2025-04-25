package common.I2P.NetworkDB;

import common.I2P.IDs.Destination;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Set;

public class LeaseSet implements JSONSerializable {
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
    Set<Lease> leases;

    public LeaseSet(JSONObject json) throws InvalidObjectException{
        deserialize(json);
    }

    /**
     * @param jsonType
     * @throws InvalidObjectException
     */
    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {

    }

    /**
     * @return
     */
    @Override
    public JSONType toJSONType() {
        return null;
    }
}
