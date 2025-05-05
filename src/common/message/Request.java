package common.message;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class Request extends Message {

    private String username;
    private String password;
    private String songname;
    private String songPath;
    private int otp;
    private int size;

    /**
     * Constructor for the user to request authentication with the server
     *
     * @param type - String type of message
     * @param username - String username of the user
     * @param password - String password of the user
     * @param otp - Integer otp of the user
     */
    public Request(String type, String username, String password, int otp) {
        super(type);
        this.username = username;
        this.password = password;
        this.otp = otp;
    }

    /**
     * Constructor for the user to request create user from the database
     *
     * @param type - String type of message
     * @param username - String username of the user
     * @param password - String password or song name of the user
     */
    public Request(String type, String username, String password) {
        super(type);
        this.username = username;
        this.password = password;
    }

    /**
     * Constructor for the user to request add song to the database
     *
     * @param type - String type of message
     * @param songname - String song name of the user
     * @param size - int size of the song
     */
    public Request(String type, String songname, int size) {
        super(type);
        this.songname = songname;
        this.size = size;
    }

    /**
     * Constructor for the user to request play song from the database
     *
     * @param type - String type of message
     * @param songname - String song name of the user
     */
    public Request(String type, String songname) {
        super(type);
        this.songname = songname;
    }

    /**
     * Constructor for the user to request something with the server
     *
     * @param jsonMessage - JSONObject to deserialize
     */
    public Request(JSONObject jsonMessage) {
        super(jsonMessage);
        if (!(super.type.equals("Create") || super.type.equals("Authenticate") ||
                super.type.equals("Add") || super.type.equals("Play") || super.type.equals("List"))) {
            throw new IllegalArgumentException("Bad type: " + super.type);
        }
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        super.deserialize(jsonType);

        JSONObject obj = (JSONObject) jsonType;

        switch (super.type) {
            case "Create":
                obj.checkValidity(new String[]{"username", "password"});
                username = obj.getString("username");
                password = obj.getString("password");
                break;
            case "Authenticate":
                obj.checkValidity(new String[]{"username", "password", "otp"});
                username = obj.getString("username");
                password = obj.getString("password");
                otp = obj.getInt("otp");
                break;
            case "Add":
                obj.checkValidity(new String[]{"songname", "size"});
                songname = obj.getString("songname");
                size = obj.getInt("size");
                break;
            case "Play":
                obj.checkValidity(new String[]{"songname"});
                songname = obj.getString("songname");
                break;
        }
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject obj = super.toJSONType();
        switch (super.type) {
            case "Create":
                obj.put("username", username);
                obj.put("password", password);
                break;
            case "Authenticate":
                obj.put("username", username);
                obj.put("password", password);
                obj.put("otp", otp);
                break;
            case "Add":
                obj.put("size", size);
                obj.put("songname", songname);
                break;
            case "Play":
                obj.put("songname", songname);
                break;
            case "List":
                break;
        }
        return obj;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSongname() {
        return songname;
    }

    public int getSize() {
        return size;
    }

    public Integer getOtp() {
        return otp;
    }
}
