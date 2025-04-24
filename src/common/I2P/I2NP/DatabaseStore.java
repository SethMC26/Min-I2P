package common.I2P.I2NP;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import javax.xml.crypto.Data;
import java.io.InvalidObjectException;

public class DatabaseStore extends I2NPMessage{
    /**
     * Type of storage {@code 1 RouterInfo}
     */
    private int storeType;

    /**
     * Create I2NPMessage from JSONObject
     *
     * @param message@throws InvalidObjectException Throws if JSONObject is not valid for type of I2NPMessage
     */
    DatabaseStore(JSONObject message) throws InvalidObjectException {
        super(message);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public JSONType toJSONType() {
        return null;
    }
}
