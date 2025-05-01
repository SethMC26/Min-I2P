package common.transport.I2CP;

import common.I2P.IDs.Destination;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

import static common.transport.I2CP.I2CPMessageTypes.CREATESESSION;

/**
 * Message sent C->R to initiate a session to some destination in the network, messages will be sent to that destination
 * and received from that destination
 */
public class CreateSession extends I2CPMessage{
    /**
     * Destination to create session for
     */
    private Destination destination;

    /**
     * Message to create session
     * @param destination Destination to setup session for
     */
    public CreateSession(Destination destination) {
        super(0, CREATESESSION); //sessionID is set later on by the router
        this.destination = destination;
    }

    CreateSession(JSONObject json) throws InvalidObjectException {
        super(json);
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType); //super class handles JSONObject check
        JSONObject json = (JSONObject) jsonType;
        json.checkValidity(new String[] {"destination"});

        destination = new Destination(json.getObject("destination"));
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = super.toJSONType();
        json.put("destination", destination.toJSONType());
        return json;
    }

    public Destination getDestination() {
        return destination;
    }
}
