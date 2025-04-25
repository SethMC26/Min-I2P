package server;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;
import java.util.concurrent.ConcurrentHashMap;

public class AudioDatabase implements JSONSerializable {

    private ConcurrentHashMap<String, Audio> audio = new ConcurrentHashMap<>();

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {

    }

    @Override
    public JSONType toJSONType() {
        return null;
    }
}
