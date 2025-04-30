package common.transport.I2CP;

import common.I2P.IDs.Destination;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

import static common.transport.I2CP.I2CPMessageTypes.CREATESESSION;

public class CreateSession extends I2CPMessage{
    private Destination destination;

    CreateSession(Destination destination, int sessionID) {
        super(0, sessionID, CREATESESSION);
    }

    CreateSession(JSONObject json) throws InvalidObjectException {
        super(json);
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType); //super class handles JSONObject check
        JSONObject json = (JSONObject) jsonType;
        destination = new Destination(json.getObject("destination"));
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = super.toJSONType();
        json.put("destination", destination.toJSONType());
        return json;
    }
}
