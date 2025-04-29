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
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class UsersDatabase implements JSONSerializable {

    private ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private File file;

    UsersDatabase(String path) {
        this.file = new File(path);
        if (!file.exists()) {
            try {
                //create directory if necessary
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdir();
                }

                if (!file.createNewFile()) {
                    System.err.println("Could not create users.json file");
                    throw new IOException("Could not create User.json file");
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
     * Adds a new user to the list of users.
     *
     * @param userName The username of the new user.
     * @param password The password of the new user.
     * @param totpKey  Base64 encoding of TOTP-key to use for HMAC in totp protocol
     */
    public boolean addUser(String userName, String password, String totpKey) {

        // Check if user already exists
        if (checkIfUserExists(userName)) {
            System.err.println("User already exists. Cannot add user.");
            return false;
        }

        // Generate a salt and hash password
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        String saltString = Base64.getEncoder().encodeToString(salt);
        String hashedPassword = encryptPass(password, salt);

        // Add new user
        users.put(userName, new User(userName, hashedPassword, saltString, totpKey));
        saveUsers();
        return true;
    }

    /**
     * Checks if a user exists in the map of users.
     *
     * @param userName The username to check for existence.
     * @return true if the user exists, false otherwise.
     */
    public boolean checkIfUserExists(String userName) {
        return users.containsKey(userName);
    }

    /**
     * Encrypts the password using SCRYPT with the given salt.
     */
    public static String encryptPass(String pass, byte[] salt) {
        byte[] passHash = org.bouncycastle.crypto.generators.SCrypt.generate(
                pass.getBytes(), salt, 2048, 8, 1, 16);
        return Base64.getEncoder().encodeToString(passHash);
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONArray)) {
            throw new InvalidObjectException("JSONArray expected.");
        }

        JSONArray obj = (JSONArray) jsonType;

        for (int i = 0; i < obj.size(); i++) {
            JSONObject userObj = obj.getObject(i);
            User newUser = new User(userObj);
            users.put(newUser.getUserName(), newUser);
        }
    }

    private void saveUsers() {
        try {
            if (users.isEmpty()) {
                // do not save a file if we have nothing to save
                return;
            }
            JsonIO.writeFormattedObject(this, file);
        } catch (FileNotFoundException e) {
            System.err.println("Error saving users file: " + e.getMessage());
        }
    }

    public User getUserByName(String userName) {
        return users.get(userName);
    }

    @Override
    public JSONType toJSONType() {
        JSONArray jsonArray = new JSONArray();

        for (User user : users.values()) {
            jsonArray.add(user.toJSONType());
        }
        return jsonArray;
    }

    @Override
    public String serialize() {
        return this.toJSONType().getFormattedJSON();
    }
}
