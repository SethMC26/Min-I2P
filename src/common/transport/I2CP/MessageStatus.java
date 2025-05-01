package common.transport.I2CP;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;

import static common.transport.I2CP.I2CPMessageTypes.MESSAGESTATUS;

/**
 * Message status notifies a client about the state of an incoming or outgoing message
 */
public class MessageStatus extends I2CPMessage{
    /**
     * Status of for message status
     * <p>{@code ACCEPTED} Message accepted by router</p>
     * <p>{@code SUCCEEDED} Message received at destination</p>
     * <p>{@code BESTEFFORTFAILURE } Message exceeded max-retransmit and was not delivered</p>
     * <p>{@code ROUTERFAILURE} Router failed fatal could not send message</p>
     * <p>{@code BADDESTINATION} Destination was bad/invalid</p>
     * <p>{@code BADLEASESET} Leaseset was bad/invalid</p>
     */
    public enum Status {
        ACCEPTED,
        SUCCEEDED,
        BESTEFFORTFAILURE,
        ROUTERFAILURE,
        BADDESTINATION,
        BADLEASESET,
    }

    /**
     * Status of message
     */
    private Status status;
    /**
     * Nonce of message
     */
    private byte[] nonce;

    /**
     * Message status
     * @param sessionID Session this message belongs to
     * @param messageID ID of message this message is for
     * @param nonce When {@code status == Status.ACCEPTED} nonce matches nonce from SendMesasage and message ID will be
     * @param status message status
     * @implSpec Nonce here is NOT used to prevent replay attacks, it is used as a number once to help identify messages
     *           use of this nonce comes purely from the spec
     */
    MessageStatus(int sessionID, int messageID, byte[] nonce, Status status) {
        super(sessionID, messageID, MESSAGESTATUS);
    }

    MessageStatus(JSONObject json) throws InvalidObjectException{
        super(json);
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType);
        JSONObject json = (JSONObject) jsonType; //type check handled in super class

        json.checkValidity(new String[] {"status", "nonce"});
        status = Status.values()[json.getInt("status")]; //get status from ordinal enum value
        nonce = Base64.decode(json.getString("nonce"));
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = super.toJSONType();
        json.put("status", status.ordinal());
        json.put("nonce", Base64.toBase64String(nonce));
        return json;
    }
}
