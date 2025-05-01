package common.transport.I2CP;

import common.I2P.IDs.Destination;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;

import static common.transport.I2CP.I2CPMessageTypes.SENDMESSAGE;

/**
 * Message with data to send/recv over a session C->D
 */
public class SendMessage extends I2CPMessage{
    /**
     * Destination for this message
     */
    private Destination destination;
    /**
     * Payload of the message for client applications
     */
    private JSONObject payload;
    /**
     * Nonce of message
     */
    private byte[] nonce;
    /**
     * Message to send to destination C->D
     * @param messageID ID of this message
     * @param sessionID Session this message belongs to
     * @param destination Destination to send message to
     * @param nonce Nonce of message - used for later MessageStatus updates
     * @param payload Payload of message
     * @implNote Nonce is NOT used to prevent message replay/MITM attacks so we will use nonce
     */
    public SendMessage(int messageID, int sessionID, Destination destination, byte[] nonce, JSONObject payload) {
        super(messageID, sessionID, SENDMESSAGE);
        this.destination = destination;
        this.nonce = nonce;
        this.payload = payload;
    }

    public SendMessage(JSONObject json) throws InvalidObjectException {
        super(json);
        deserialize(json);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType); //super handles type check
        JSONObject json = (JSONObject) jsonType;

        json.checkValidity(new String[] {"destination", "payload", "nonce"});
        destination = new Destination(json.getObject("destination"));
        payload = json.getObject("payload");
        nonce = Base64.decode(json.getString("nonce"));
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject json = super.toJSONType();
        json.put("destination", destination.toJSONType());
        json.put("payload", payload.toJSON());
        json.put("nonce", Base64.toBase64String(nonce));
        return json;
    }
}
