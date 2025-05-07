package server.databases;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AudioDatabase implements JSONSerializable {

    private ConcurrentHashMap<String, Audio> audioList = new ConcurrentHashMap<>();
    private File file;
    private String audioFilePath;

    public AudioDatabase(String path, String audioFilePath) {
        this.audioFilePath = audioFilePath;
        this.file = new File(path);
        if (!(file.length() > 0)) {
            return;
        }
        else if (!file.exists()) {
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
    }

    public String getAudio(String audioName) {
        String audioPath = audioList.get(audioName).getAudio();

        return audioPath;

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
    public void addAudio(String audioName, List<byte[]> audio, int size) {
        if (checkIfAudioExists(audioName)) {
            System.err.println("Audio already exists in the database");
            return;
        }
        saveAudioFile(audioFilePath + audioName + ".txt", audio);
        Audio newAudio = new Audio(audioName, audioFilePath + audioName + ".txt", size);
        audioList.put(audioName, newAudio);
        saveUsers();
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONArray)) {
            throw new InvalidObjectException("Audio JSON is not a JSONArray");
        }

        JSONArray array = (JSONArray) jsonType;

        for (int i = 0; i < array.size(); i++) {
            JSONObject obj =  array.getObject(i);
            if (obj == null) {
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

    private void saveAudioFile(String path, List<byte[]> audio) {

        File file = new File(path);
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
        }

        try (FileWriter fw = new FileWriter(path,true)) {

            for (byte[] base64Bytes : audio) {
                if (base64Bytes != null) {
                    String text = Base64.toBase64String(base64Bytes) + "\n";
                    fw.write(text);
                    fw.flush();
                }
            }

        } catch (IOException e) {
            System.err.println("Error saving audio file: " + e.getMessage());
        }

    }
}
