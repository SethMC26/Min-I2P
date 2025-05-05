package common.I2P.I2NP;

import common.I2P.IDs.RouterID;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;
import java.security.PublicKey;

public class EndpointPayload implements JSONSerializable {
    private int tunnelID;
    private RouterID routerID;

    private JSONObject jsonObject;

    public EndpointPayload(int tunnelID, RouterID routerID, JSONObject jsonObject) {
        this.tunnelID = tunnelID;
        this.routerID = routerID;
        this.jsonObject = jsonObject;
    }
    public EndpointPayload(JSONObject jsonObject) {
        try {
            deserialize(jsonObject);
        } catch (InvalidObjectException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public int getTunnelID() {
        return tunnelID;
    }

    public RouterID getRouterID() {
        return routerID;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void encryptPayload(PublicKey pk) {
        // only encrypt the payload, not the whole object
    }

    public void decryptPayload(PublicKey pk) {
        // only decrypt the payload, not the whole object
    }

    public void encryptWhole() {
        // encrypt the whole endpoint payload
    }

    public void decryptWhole() {
        // decrypt the whole endpoint payload
    }

    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        if (!(arg0 instanceof JSONObject)) {
            throw new InvalidObjectException("Must be JSONObject");
        }
        JSONObject jsonObject = (JSONObject) arg0;
        jsonObject.checkValidity(new String[] { "tunnelID", "routerID", "payload" });
        //todo wtf why did this fix our issues
        System.err.println(jsonObject.getFormattedJSON());
        Object test = jsonObject.get("tunnelID");
        System.out.println(test);
        this.tunnelID = (Integer) test;
        this.routerID = new RouterID(jsonObject.getObject("routerID"));
        this.jsonObject = jsonObject.getObject("payload"); // pray this works
    }

    @Override
    public JSONObject toJSONType() {
        // TODO Auto-generated method stub
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tunnelID", tunnelID);
        jsonObject.put("routerID", routerID.toJSONType());
        jsonObject.put("payload", this.jsonObject);
        return jsonObject;
    }


}
