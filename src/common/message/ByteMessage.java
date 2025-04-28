package common.message;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;

public class ByteMessage extends Message {

    private byte[] data;

    public ByteMessage(String type, byte[] data) {
        super(type);
        this.data = data;
    }

    public ByteMessage(JSONObject jsonMessage) {
        super(jsonMessage);
        if (!super.type.equals("Byte")) {
            throw new IllegalArgumentException("Bad type: " + super.type);
        }
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject)) {
            throw new InvalidObjectException("JSONObject expected.");
        }

        JSONObject obj = (JSONObject) jsonType;

        super.deserialize(obj);

        obj.checkValidity(new String[]{"type", "data"});
        String temp = obj.getString("data");
        data = Base64.decode(temp);
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject obj = super.toJSONType();

        obj.put("data", Base64.toBase64String(data));

        return obj;
    }

    public byte[] getData() {
        return data;
    }
}
