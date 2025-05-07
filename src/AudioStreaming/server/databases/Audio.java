package AudioStreaming.server.databases;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class Audio implements JSONSerializable {

    private String audioName;
    private String audio;
    private int size;

    /**
     * This is the constructor for one audio of the bulletin board database
     *
     * @param audioName - String the name of the audio
     * @param audio     - String of the audio's path location
     */
    Audio(String audioName, String audio, int size) {
        this.audioName = audioName;
        this.audio = audio;
        this.size = size;
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

    /**
     * Gets the audio name of this audio
     *
     * @return - String audio name
     */
    public String getName() {
        return audioName;
    }

    /**
     * Gets the audio of this audio
     *
     * @return - String audio path location
     */
    public String getAudio() {
        return audio;
    }

    /**
     * Gets the size of this audio
     *
     * @return - int size of the audio
     */
    public int getSize() {
        return size;
    }


    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject)) {
            throw new InvalidObjectException("Audio JSON is not a JSONObject");
        }

        JSONObject obj = (JSONObject) jsonType;

        obj.checkValidity(new String[]{"audioName", "audio", "size"});

        this.audioName = obj.getString("audioName");
        this.audio = obj.getString("audio");
        this.size = obj.getInt("size");
    }

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("audioName", audioName);
        obj.put("audio", audio);
        obj.put("size", size);
        return obj;
    }
}
