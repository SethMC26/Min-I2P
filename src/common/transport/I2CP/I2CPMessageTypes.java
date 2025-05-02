package common.transport.I2CP;

/**
 * Types of I2CPMessages
 * <p>{@code CREATESESSION} - create session message</p>
 * <p>{@code SESSIONSTATUS} - Status of session </p>
 * <p>{@code REQUESTLEASESET} - not implemented </p>
 * <p>{@code CREATELEASESET} - not implemented</p>
 * <p>{@code DESTLOOKUP} lookup a destination</p>
 * <p>{@code DESTREPLY} get reply of lookup</p></p>
 * <p>{@code SENDMESSAGE} - Send data message to destination</p>
 * <p>{@code MESSAGESTATUS} - Status of sent message </p>
 * <p>{@code DESTROYSESSION} - Destroy session</p>
 */
public enum I2CPMessageTypes {
    CREATESESSION,
    SESSIONSTATUS,
    REQUESTLEASESET,
    CREATELEASESET,
    DESTLOOKUP,
    DESTREPLY,
    SENDMESSAGE,
    PAYLOADMESSAGE,
    MESSAGESTATUS,
    DESTROYSESSION
}
