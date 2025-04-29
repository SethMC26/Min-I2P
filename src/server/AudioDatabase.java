package server;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.concurrent.ConcurrentHashMap;

public class AudioDatabase implements JSONSerializable {

    private ConcurrentHashMap<String, Audio> audioList = new ConcurrentHashMap<>();
    private File file;

    AudioDatabase(String path) {
        this.file = new File(path);
        if (!file.exists()) {
            try {
                //create directory if necessary
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdir();
                }

                if (!file.createNewFile()) {
                    System.err.println("Could not create audio.json file");
                    throw new IOException("Could not create audio.json file");
                }
            } catch (IOException e) {
                System.err.println("FATAL: Could not create database: " + e.getMessage());
            }
        } else {
            try {
                // Read and deserialize JSON data
                deserialize(JsonIO.readArray(file));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(
                        "File not found. Please ensure the file exists at: " + file.getAbsolutePath(), e);
            } catch (SecurityException e) {
                throw new RuntimeException(
                        "Permission denied. Please check file permissions for: " + file.getAbsolutePath(), e);
            } catch (InvalidObjectException e) {
                throw new RuntimeException("Invalid object in users file. Please check the file format.", e);
            }
        }

        if (file.length() == 0)
            return;

        try {
            // Read and deserialize JSON data
            deserialize(JsonIO.readArray(file));
        }  catch (Exception e) {
            System.err.println("Error reading users file: " + e.getMessage());
        }
    }

    /**
     * Checks if the song name exists in the database
     *
     * @param audioName - String the name of the audio
     * @return - boolean true if the audio exists, false otherwise
     */
    public boolean checkIfAudioExists(String audioName) {
        return audioList.containsKey(audioName);
    }

    /**
     * Lists all the audio in the database
     *
     * @return - String of all the audio names
     */
    public String listAudio() {
        StringBuilder audioListString = new StringBuilder();
        for (String key : audioList.keySet()) {
            audioListString.append(key).append(",");
        }
        return audioListString.toString();
    }

    /**
     * Adds the audio to the database
     *
     * @param audioName - String the name of the audio
     * @param audio - String of the audio
     */
    public boolean addAudio(String audioName, String audio) {
        if (checkIfAudioExists(audioName)) {
            System.err.println("Audio already exists in the database");
            return false;
        }
        Audio newAudio = new Audio(audioName, audio);
        audioList.put(audioName, newAudio);
        saveUsers();
        return true;
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONArray)) {
            throw new InvalidObjectException("Audio JSON is not a JSONArray");
        }

        JSONArray array = (JSONArray) jsonType;

        for (int i = 0; i < array.size(); i++) {
            JSONType obj = (JSONObject) array.get(i);
            if (!(obj instanceof JSONObject)) {
                throw new InvalidObjectException("Audio JSON is not a JSONObject");
            }
            Audio audio = new Audio(obj);
            audioList.put(audio.getName(), audio);
        }
    }

    @Override
    public JSONType toJSONType() {
        JSONArray array = new JSONArray();
        for (Audio audio : audioList.values()) {
            array.add(audio.toJSONType());
        }
        return array;
    }

    @Override
    public String serialize() {
        return this.toJSONType().getFormattedJSON();
    }

    private void saveUsers() {
        try {
            if (audioList.isEmpty()) {
                // do not save a file if we have nothing to save
                return;
            }
            JsonIO.writeFormattedObject(this, file);
        } catch (FileNotFoundException e) {
            System.err.println("Error saving users file: " + e.getMessage());
        }
    }
}
