package common.I2P.NetworkDB;

import common.I2P.IDs.RouterID;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;

/**
 * RouterInfo Defines all of the data that a router wants to publish for the network to see
 *
 * @implNote Spec has addresses as an array but I think for our case we will just have each router have a single address
 */
public class RouterInfo extends Record implements JSONSerializable {
    /**
     * Router ID of this router
     */
    private RouterID routerID;

    /**
     * Date published in epoch time
     */
    private long date;

    /**
     * RouterAddress for RouterInfo
     */
    private RouterAddress routerAddress;
    /**
     * Signature of this RouterIndo from corresponding private key in RouterID
     */
    private byte[] signature;

    /**
     * Create new RouterInfo Object
     * @param routerID ID of router that this info belongs to
     * @param date Date RouterInfo was published
     * @param host Host of router's address
     * @param port Port to Router is listening to
     * @param signingKey PrivateKey to use to sign this RouterID
     */
    public RouterInfo(RouterID routerID, long date, String host, int port, PrivateKey signingKey) {
        super(RecordType.ROUTERINFO);
        this.routerID = routerID;
        this.date = date;
        this.routerAddress = new RouterAddress(host,port);

        try {
            //get signature ready
            Signature signing = Signature.getInstance("Ed25519");
            signing.initSign(signingKey);
            //sign this RouterInfo
            signing.update(routerID.getElgamalPublicKey().getEncoded());
            signing.update(routerID.getSigningPublicKey().getEncoded());
            signing.update(ByteBuffer.allocate(Long.BYTES).putLong(date).array());
            signing.update(host.getBytes(StandardCharsets.UTF_8));
            signing.update(ByteBuffer.allocate(Integer.BYTES).putInt(port).array());
            //get signature
            this.signature = signing.sign();
        } catch (NoSuchAlgorithmException | SignatureException e) {
            throw new RuntimeException(e); //should never hit case
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Bad private key for Ed25519 " + e);
        }
    }

    /**
     * Create RouterInfo from json
     * @param json
     * @throws InvalidObjectException
     */
    public RouterInfo(JSONObject json) throws InvalidObjectException {
        super(RecordType.ROUTERINFO);
        deserialize(json);
    }

    @Override
    public byte[] getHash() {
        return routerID.getHash();
    }

    /**
     * Verify the signature of this record given a public key
     * @return true if signature is valid false otherwise
     */
    @Override
    public boolean verifySignature() {
        try {
            //get signature ready
            Signature signing = Signature.getInstance("Ed25519");
            signing.initVerify(routerID.getSigningPublicKey());
            //update signature in same order we created signature
            signing.update(routerID.getElgamalPublicKey().getEncoded());
            signing.update(routerID.getSigningPublicKey().getEncoded());
            signing.update(ByteBuffer.allocate(Long.BYTES).putLong(date).array());
            signing.update(routerAddress.host.getBytes(StandardCharsets.UTF_8));
            signing.update(ByteBuffer.allocate(Integer.BYTES).putInt(routerAddress.port).array());

            return signing.verify(signature);
        } catch (NoSuchAlgorithmException | SignatureException e) {
            throw new RuntimeException(e); //should never hit case
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Bad private key for SHA1withDSA" + e);
        }
    }

    /**
     * Deserialize a JSON of RouterInfo
     * @param jsonType JSONObject of RouterInfo
     * @throws InvalidObjectException throws if JSON is invalid
     */
    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("Type must be JSONObject");

        JSONObject json = (JSONObject) jsonType;
        json.checkValidity(new String[] {"routerID", "date", "routerAddress", "signature"});

        routerID = new RouterID(json.getObject("routerID"));
        date = json.getLong("date"); //gosh this get long sure is something huh
        routerAddress = new RouterAddress(json.getObject("routerAddress"));
        signature = Base64.decode(json.getString("signature"));
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = new JSONObject();
        json.put("routerID", routerID.toJSONType());
        json.put("date", date);
        json.put("routerAddress", routerAddress.toJSONType());
        json.put("signature", Base64.toBase64String(signature));
        return json;
    }

    public RouterID getRouterID() {
        return routerID;
    }

    public byte[] getSignature() {
        return signature;
    }

    public long getDate() {
        return date;
    }

    public int getPort() {
        return routerAddress.port;
    }

    public String getHost() {
        return routerAddress.host;
    }

    /**
     * Address(s) used by router we just use a host and port
     */
    private class RouterAddress implements JSONSerializable {
        private String host;
        private int port;

        RouterAddress(JSONObject jsonObject) throws InvalidObjectException {
            deserialize(jsonObject);
        }

        RouterAddress(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public void deserialize(JSONType jsonType) throws InvalidObjectException {
            if (!(jsonType instanceof JSONObject))
                throw new InvalidObjectException("RouterAddress must be a jsonObject");

            JSONObject routerAddressJSON = (JSONObject) jsonType;
            routerAddressJSON.checkValidity(new String[] {"host", "port"});
            host = routerAddressJSON.getString("host");
            port = routerAddressJSON.getInt("port");
        }

        @Override
        public JSONObject toJSONType() {
            JSONObject routerAddressJSON = new JSONObject();
            routerAddressJSON.put("host", host);
            routerAddressJSON.put("port", port);
            return routerAddressJSON;
        }
    }
}
