package common.transport.I2CP;

import merrimackutil.json.types.JSONObject;

import java.io.InvalidObjectException;

import static common.transport.I2CP.I2CPMessageTypes.DESTROYSESSION;

public class DestroySession extends I2CPMessage {
    /**
     * Destroy Session for this ID
     * @param sessionID
     */
    public DestroySession(int sessionID ) {
        super(sessionID, DESTROYSESSION);
    }

    public DestroySession(JSONObject json) throws InvalidObjectException {
        super(json);
    }
}
