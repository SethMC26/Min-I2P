package AudioStreaming.message;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;

public class ByteMessage extends Message {

    private byte[] data;
    private int id;

    /**
     * Constructor for the user to send a byte message
     *
     * @param type - String type of message
     * @param data - byte[] data of the message
     */
    public ByteMessage(String type, String desthash, byte[] data, int id) {
        super(type, desthash);
        this.data = data;
        this.id = id;
    }

    /**
     * Constructor for the user to send a byte message
     *
     * @param jsonMessage - JSONObject to deserialize
     */
    public ByteMessage(JSONObject jsonMessage) {
        super(jsonMessage);
        if (!super.type.equals("Byte")) {
            throw new IllegalArgumentException("Bad type: " + super.type);
        }
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType);

        JSONObject obj = (JSONObject) jsonType;

        obj.checkValidity(new String[]{"data", "id"});

        String temp = obj.getString("data");
        this.id = obj.getInt("id");
        data = Base64.decode(temp);
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject obj = super.toJSONType();

        obj.put("data", Base64.toBase64String(data));
        obj.put("id", id);

        return obj;
    }

    public byte[] getData() {
        return data;
    }

    public int getId() {
        return id;
    }
}
