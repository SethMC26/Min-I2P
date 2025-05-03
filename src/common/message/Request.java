package common.message;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class Request extends Message {

    private String username;
    private String password;
    private String songname;
    private String songPath;
    private Integer otp;

    /**
     * Constructor for the user to request authentication with the server
     *
     * @param type - String type of message
     * @param username - String username of the user
     * @param password - String password of the user
     * @param otp - Integer otp of the user
     */
    public Request(String type, String username, String password, Integer otp) {
        super(type);
        this.username = username;
        this.password = password;
        this.otp = otp;
    }

    /**
     * Constructor for the user to request create user, or play, or list songs from the database
     *
     * @param type - String type of message
     * @param username - String username of the user
     * @param passOrSong - String password or song name of the user
     */
    public Request(String type, String username, String passOrSong) {
        super(type);
        this.username = username;
        if (type.equals("Create")) {
            this.password = passOrSong;
        } else if (type.equals("Add") || type.equals("Play") || type.equals("List")) {
            this.songname = passOrSong;
        }
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

        obj.checkValidity(new String[]{"type", "username"});
        username = obj.getString("username");

        switch (super.type) {
            case "Create":
                obj.checkValidity(new String[]{"password"});
                password = obj.getString("password");
                break;
            case "Authenticate":
                obj.checkValidity(new String[]{"password", "otp"});
                password = obj.getString("password");
                otp = obj.getInt("otp");
                break;
            case "Add", "Play", "List":
                obj.checkValidity(new String[]{"songname"});
                songname = obj.getString("songname");
                break;
        }
    }

    @Override
    public JSONObject toJSONType() {
        JSONObject obj = super.toJSONType();
        obj.put("username", username);

        switch (super.type) {
            case "Create":
                obj.put("password", password);
                break;
            case "Authenticate":
                obj.put("password", password);
                obj.put("otp", otp);
                break;
            case "Add", "Play", "List":
                obj.put("songname", songname);
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

    public Integer getOtp() {
        return otp;
    }
}
