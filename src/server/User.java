package server;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import org.bouncycastle.util.encoders.Base64;

import java.io.InvalidObjectException;
import java.nio.charset.StandardCharsets;

public class User implements JSONSerializable {
    private String userName;
    private String password;
    private String pubKey;
    private String salt;
    private String totpKey;

    /**
     * Create a new User
     * @param userName String User name
     * @param password String User's password
     * @param pubKey String base64 elgamal public key
     * @param salt String base64 encoded salt
     * @param totpKey String Base32 encoded totp key
     */
    public User(String userName, String password, String pubKey, String salt, String totpKey) {
        this.userName = userName;
        this.password = password;
        this.pubKey = pubKey;
        this.salt = salt;
        this.totpKey = totpKey;
    }

    /**
     * Creates a User Object from a JSON of user
     * @param json JSONObject with representing user class
     * @throws InvalidObjectException
     */
    public User(JSONObject json) throws InvalidObjectException {
        deserialize(json);
    }

    /**
     * Check if the password matches that on file for a user
     * @param pass String pass to test
     * @return True if password hashes match false otherwise
     */
    public boolean checkPassoword(String pass) {
        //compute test pass with salt on file
        byte[] hashedPass = org.bouncycastle.crypto.generators.SCrypt.generate(
                pass.getBytes(StandardCharsets.UTF_8), Base64.decode(salt), 2048, 8, 1, 16);

        String encodedHashedPass = Base64.toBase64String(hashedPass);
        return encodedHashedPass.equals(password);
    }

    /**
     * Deserializes JSON of user
     * @param jsonType JSONObject to deserialize
     * @throws InvalidObjectException Throws if jsontype is not a JSONObject or if JSON does not have correct keys
     */
    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("JSONObjecet expected.");

        JSONObject userJSON = (JSONObject) jsonType;

        userJSON.checkValidity(new String[]{"user", "pass", "pubkey", "salt", "totp-key"});

        userName = userJSON.getString("user");
        password = userJSON.getString("pass");
        pubKey = userJSON.getString("pubkey");
        salt = userJSON.getString("salt");
        totpKey = userJSON.getString("totp-key");
    }

    /**
     * Turn this object into a JSONObject representation
     * @return JSONObject of this user
     */
    public JSONObject toJSONType() {
        JSONObject user = new JSONObject();
        user.put("user", userName);
        user.put("pass", password);
        user.put("salt", salt);
        user.put("pubkey", pubKey);
        user.put("totp-key", totpKey);
        return user;
    }

    /**
     * Get username of user
     * @return String username
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Get Public key of user
     * @return String base64 of users Elgamal Public Key
     */
    public String getPublicKey() {
        return pubKey;
    }

    /**
     * Get totp key on file for user
     * @return String Base64
     */
    public String getTotpKey() {
        return totpKey;
    }
}
