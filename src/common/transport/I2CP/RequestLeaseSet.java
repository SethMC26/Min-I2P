package common.transport.I2CP;

import common.I2P.NetworkDB.Lease;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;
import java.util.ArrayList;

public class RequestLeaseSet extends I2CPMessage {
    /**
     * Leases router wishes to authorize
     */
    private ArrayList<Lease> leases;

    /**
     * Router send to Client to authorize set of tunnels
     * @param sessionID SessionID this message belongs to
     * @param leases Lease router wishes client to authorize
     */
    public RequestLeaseSet(int sessionID, ArrayList<Lease> leases) {
        super(sessionID, I2CPMessageTypes.REQUESTLEASESET);
        this.leases = leases;
    }

    public RequestLeaseSet(JSONObject json) throws InvalidObjectException {
        super(json);
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType); //super class handles type check
        JSONObject json = (JSONObject) jsonType;
        json.checkValidity(new String[] {"leases"});

        leases = new ArrayList<>(); //create new array to hold leases
        JSONArray array = json.getArray("leases"); //get array of leases
        for(int i = 0 ; i < array.size() ; i++) {
            System.err.println("Found lease");
            //add all leases in json array
            JSONObject obj = array.getObject(i);
            leases.add(new Lease(obj));
        }
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = super.toJSONType();
        JSONArray array = new JSONArray();
        for (Lease lease : leases) {
            array.add(lease.toJSONType());
        }
        json.put("leases", array);
        return json;
    }

    public ArrayList<Lease> getLeases() {
        return leases;
    }
}


