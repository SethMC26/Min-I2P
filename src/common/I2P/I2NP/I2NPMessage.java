package common.I2P.I2NP;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;

import java.io.InvalidObjectException;

public abstract class I2NPMessage implements JSONSerializable {
    /**
     * Create I2NPMessage from JSONObject
     * @param messageJSON Message JSON to deserialize
     * @throws InvalidObjectException Throws if JSONObject is not valid for type of I2NPMessage
     */
    I2NPMessage(JSONObject messageJSON) throws InvalidObjectException {
        deserialize(messageJSON);
    }
}
