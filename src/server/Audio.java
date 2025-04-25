package server;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class Audio implements JSONSerializable {

    private String audioName;
    private String audio;

    /**
     * This is the constructor for one audio of the bulletin board database
     *
     * @param audioName - String the name of the audio
     * @param audio     - String of the audio
     */
    Audio(String audioName, String audio) {
        this.audioName = audioName;
        this.audio = audio;
    }

    /**
     * Creates an Audio Object from a JSON of audio
     *
     * @param json JSONObject with representing audio class
     * @throws InvalidObjectException
     */
    Audio(JSONType json) throws InvalidObjectException {
        deserialize(json);
    }


    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {

    }

    @Override
    public JSONType toJSONType() {
        return null;
    }
}
