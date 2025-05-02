package common.transport.I2CP;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

import static common.transport.I2CP.I2CPMessageTypes.SESSIONSTATUS;

public class SessionStatus extends I2CPMessage{
    /**
     * Status of a session based as given in Session Status message
     * <p>{@code DESTROYED(0)} Session for {@code sessionID} is terminated </p>
     * <p>{@code CREATED(1)} Session for a new given {@code sessionID} is now active </p>
     * <p>{@code UPDATED(2)} Session for {@code sessionID} has been reconfigured</p>
     * <p>{@code INVALID(3) } Session for {@code sessionID} Configuration is invalid</p>
     * <p>{@code REFUSED(4) } Router was unable to create session, {@code sessionID } should be ignored</p>
     */
    public enum Status {
        DESTROYED,
        CREATED,
        UPDATED,
        INVALID,
        REFUSED
    }

    /**
     * Status of session
     */
    private Status status;

    /**
     * Construct a new SessionStatus message
     * @param sessionID Session this message belongs to
     * @param status Status of this message
     */
    public SessionStatus(int sessionID, Status status) {
        super(sessionID, SESSIONSTATUS);
        this.status = status;
    }

    public SessionStatus(JSONObject json) throws InvalidObjectException{
        super(json);
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType); //check handled by super class
        JSONObject json = (JSONObject) jsonType;
        json.checkValidity(new String[] {"status"});
        status = Status.values()[json.getInt("status")];
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = super.toJSONType();
        json.put("status", status.ordinal());
        return json;
    }

    public Status getStatus() {
        return status;
    }
}
