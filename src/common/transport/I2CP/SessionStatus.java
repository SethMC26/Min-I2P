package common.transport.I2CP;

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
    private Status status;

    public SessionStatus(int sessionID, Status status) {
        super(1, sessionID, SESSIONSTATUS);
        this.status = status;
    }

}
