package common.I2P.NetworkDB;

import common.I2P.IDs.RouterID;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * RouterInfo Defines all of the data that a router wants to publish for the network to see
 *
 * @implNote Spec has addresses as an array but I think for our case we will just have each router have a single address
 */
public class RouterInfo implements JSONSerializable {
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
     * Create new RouterInfo Object
     * @param routerID ID of router that this info belongs to
     * @param date Date RouterInfo was published
     * @param host Host of router's address
     * @param port Port to Router is listening to
     */
    RouterInfo(RouterID routerID, long date, String host, int port) {
        this.routerID = routerID;
        this.date = date;
        this.routerAddress = new RouterAddress(host,port);
    }

    /**
     * Create RouterInfo from json
     * @param json
     * @throws InvalidObjectException
     */
    public RouterInfo(JSONObject json) throws InvalidObjectException {
        deserialize(json);
    }

    /**
     * Get SHA256 hash of this class
     * @return 32 byte SHA256 hash of this class
     */
    public byte[] getHash() {
        try {
            //hash this class
            MessageDigest md = MessageDigest.getInstance("SHA256");
            //update hash with router info
            md.update(routerID.getElgamalPublicKey().getEncoded());
            md.update(routerID.getDSASHA1PublicKey().getEncoded());
            //update hash with date
            ByteBuffer longBytes = ByteBuffer.allocate(Long.BYTES);
            longBytes.putLong(date);
            md.update(longBytes);
            //update hash with host
            md.update(routerAddress.host.getBytes(StandardCharsets.UTF_8));
            //update hash with port
            ByteBuffer portByte = ByteBuffer.allocate(Integer.BYTES);
            portByte.putInt(routerAddress.port);
            md.update(portByte);

            return md.digest();
        }
        catch (NoSuchAlgorithmException ex) {throw new RuntimeException(ex);} //should not hit this case
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

        routerID = new RouterID(json.getObject("routerID"));
        date = json.getLong("date"); //gosh this get long sure is something huh
        routerAddress = new RouterAddress(json.getObject("routerAddress"));
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = new JSONObject();
        json.put("routerID", routerID.toJSONType());
        json.put("date", date);
        json.put("routerAddress", routerAddress.toJSONType());
        return json;
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
