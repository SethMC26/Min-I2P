package common.transport.I2CP;

/**
 * Types of I2CPMessages
 * <p>{@code CREATESESSION} - create session message</p>
 * <p>{@code SESSIONSTATUS} - Status of session </p>
 * <p>{@code REQUESTLEASESET} - not implemented </p>
 * <p>{@code CREATELEASESET} - not implemented</p>
 * <p>{@code SENDMESSAGE} - Send data message to destination</p>
 * <p>{@code MESSAGESTATUS} - Status of sent message </p>
 */
public enum I2CPMessageTypes {
    CREATESESSION,
    SESSIONSTATUS,
    REQUESTLEASESET, //not implemented
    CREATELEASESET, //not implemented
    SENDMESSAGE,
    MESSAGESTATUS
}
