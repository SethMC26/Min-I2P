package common.I2P.I2NP;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

/**
 * A simple message acknowledgment
 */
public class DeliveryStatus extends I2NPMessage{
    /**
     * Unique ID of the message we are delivering the deliveryStatus for
     */
    private int msgID;
    /**
     * Epoch time of when message was successfully created/delivered
     */
    private long timestamp;

    /**
     * Create Deliver Status from json
     * @param json JSON to deserialize
     * @throws InvalidObjectException throws if json is invalid
     */
    DeliveryStatus(JSONObject json) throws InvalidObjectException{
        deserialize(json);
    }

    /**
     * Create Delivery Status message
     * @param msgID msg ID of the message for this delivery static
     * @param timestamp Epoch time of when message was successfully created/delivered
     */
    public DeliveryStatus(int msgID, long timestamp) {
        this.msgID = msgID;
        this.timestamp = timestamp;
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("Must be JSONObject");

        JSONObject json = (JSONObject) jsonType;

        msgID = json.getInt("msgID");
        timestamp = json.getLong("timestamp");
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = new JSONObject();
        json.put("msgID", msgID);
        json.put("timestamp", timestamp);
        return json;
    }


}
