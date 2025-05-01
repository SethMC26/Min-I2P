package common.transport.I2CP;

import common.I2P.IDs.Destination;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

import static common.transport.I2CP.I2CPMessageTypes.DESTREPLY;

public class DestinationReply extends I2CPMessage{
    /**
     * Destination found in lookup or null if not found
     */
    private Destination destination;

    /**
     * Destination Reply message
     * @param sessionID ID of session this message belongs
     * @param destination Destination found or null if no destination found
     */
    public DestinationReply(int sessionID, Destination destination) {
        super(sessionID, DESTREPLY);
        this.destination = destination;
    }

    public DestinationReply(JSONObject json) throws InvalidObjectException {
        super(json);
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType);
        JSONObject json = (JSONObject) jsonType;
        if (json.containsKey("destination")) {
            destination = new Destination(json.getObject("destination"));
        }
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = super.toJSONType();

        if (destination != null ) {
            json.put("destination", destination.toJSONType());
        }

        return json;
    }

    /**
     * Get destination
     * @return Destination or null if no destination was found
     * @apiNote Will be null if no destination was found during lookup
     */
    public Destination getDestination() {
        return destination;
    }
}
