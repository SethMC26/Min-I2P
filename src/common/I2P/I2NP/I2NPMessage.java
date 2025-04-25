package common.I2P.I2NP;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;

import java.io.InvalidObjectException;

/**
 * Abstract class for I2NPMessage
 *
 * @apiNote when sending an I2NPMessage it should be wrapped in an I2NPHeader
 * @code I2NPHeader I2NPex = new I2NPHeader(I2NPHeader.TYPE.{TYPE}, 1111, System.currentTimeMillis(), {I2NPMessage});}
 */
public abstract class I2NPMessage implements JSONSerializable {
    /**
     * Create I2NPMessage from JSONObject
     * @param messageJSON Message JSON to deserialize
     * @throws InvalidObjectException Throws if JSONObject is not valid for type of I2NPMessage
     */
    I2NPMessage(JSONObject messageJSON) throws InvalidObjectException {
        deserialize(messageJSON);
    }

    I2NPMessage() {};
}
