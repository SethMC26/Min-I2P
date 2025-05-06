package common.I2P.I2NP;

import common.I2P.IDs.RouterID;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.bouncycastle.util.encoders.Base64;

public class EndpointPayload implements JSONSerializable {
    private int tunnelID;
    private byte[] routerID; // hash of the routerID, not the whole object
    private byte[] encTunnelID;
    private byte[] encRouterID;

    private JSONObject jsonObject;
    private byte[] encMessage;

    public EndpointPayload(int tunnelID, byte[] routerID, JSONObject jsonObject) {
        this.tunnelID = tunnelID;
        this.routerID = routerID;
        this.jsonObject = jsonObject;
    }

    public EndpointPayload(int tunnelID, byte[] routerID, byte[] encMessage) {
        // not sure if i even need this but just in case...
        this.tunnelID = tunnelID;
        this.routerID = routerID;
        this.encMessage = encMessage;
    }

    public EndpointPayload(byte[] encTunnelID, byte[] encRouterID, byte[] encMessage) {
        this.encTunnelID = encTunnelID;
        this.encRouterID = encRouterID;
        this.encMessage = encMessage;
    }

    public EndpointPayload(JSONObject jsonObject) {
        try {
            deserialize(jsonObject);
        } catch (InvalidObjectException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public int getTunnelID() {
        return tunnelID;
    }

    public byte[] getRouterID() {
        return routerID;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public byte[] getEncTunnelID() {
        return encTunnelID;
    }

    public byte[] getEncRouterID() {
        return encRouterID;
    }

    public byte[] getEncMessage() {
        return encMessage;
    }

    /**
     * First stage of encryption with the ElGamal algorithm. This method encrypts
     * the
     * payload of the EndpointPayload object using the public key provided.
     * 
     * @param pk
     */
    public void encryptPayload(PublicKey pk) {
        // only encrypt the payload, not the whole object
        try {
            // Convert the payload (jsonObject) to bytes
            byte[] payloadBytes = jsonObject.toString().getBytes("UTF-8");

            // Encrypt the payload using ElGamal
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("ElGamal");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, pk);
            byte[] encryptedPayload = cipher.doFinal(payloadBytes);

            // Replace the jsonObject with the encrypted payload
            this.jsonObject = null;
            this.encMessage = encryptedPayload;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method decrypts the payload of the EndpointPayload object using the
     * secret key provided. This should only be used after all AES layers
     * have been stripped away.
     * 
     * @param sk
     */
    public void decryptPayload(SecretKey sk) {
        // only decrypt the payload, not the whole object
        try {
            // Decrypt the payload using ElGamal
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("ElGamal");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, sk);
            byte[] decryptedPayload = cipher.doFinal(encMessage);

            // Convert the decrypted bytes back to a JSONObject
            jsonObject = JsonIO.readObject(new String(decryptedPayload, "UTF-8"));

        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | UnsupportedEncodingException e) {
            System.err.println("Error decrypting payload: " + e.getMessage());
            e.printStackTrace();

        }
    }

    /**
     * Second stage of encryption encryption. This methods uses AES to encrypt the
     * tunnelID and routerID AND the already encrypted payload.
     * This should only be used after the ElGamal encryption has been applied to the
     * payload.
     * 
     * Please make sure to use final layer decrypt at the end point to assign values
     * properly :D
     * 
     * @param sk
     * @param iv
     */
    public void firstLayerEncrypt(SecretKey sk, byte[] iv) {
        // Encrypt the tunnelID and routerID using ElGamal
        try {
            Cipher enc1 = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec1 = new GCMParameterSpec(128, iv);
            enc1.init(Cipher.ENCRYPT_MODE, sk, gcmSpec1);
            encTunnelID = enc1.doFinal(ByteBuffer.allocate(4).putInt(tunnelID).array());

            Cipher enc2 = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec2 = new GCMParameterSpec(128, iv);
            enc1.init(Cipher.ENCRYPT_MODE, sk, gcmSpec2);
            encRouterID = enc2.doFinal(routerID);

            // Encrypt the already encrypted payload using AES
            Cipher enc3 = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec3 = new GCMParameterSpec(128, iv);
            enc1.init(Cipher.ENCRYPT_MODE, sk, gcmSpec3);
            encMessage = enc3.doFinal(encMessage);

            this.tunnelID = -1;
            this.routerID = null;

        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | InvalidAlgorithmParameterException e) {
            System.err.println("Error encrypting tunnelID: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method decrypts the tunnelID and routerID using the secret key provided.
     * This should only be used after the AES layers have been stripped away.
     * 
     * Takes a layer off the payload as well - next it will need to be elgamal
     * decrypted.
     * 
     * @param sk
     */
    public void finalLayerDecrypt(SecretKey sk, byte[] iv) {
        // Decrypt the tunnelID and routerID using ElGamal
        try {
            Cipher dec1 = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec1 = new GCMParameterSpec(128, iv);
            dec1.init(Cipher.DECRYPT_MODE, sk, gcmSpec1);
            byte[] decryptedTunnelID = dec1.doFinal(encTunnelID);
            this.tunnelID = ByteBuffer.wrap(decryptedTunnelID).getInt();

            Cipher dec2 = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec2 = new GCMParameterSpec(128, iv);
            dec2.init(Cipher.DECRYPT_MODE, sk, gcmSpec2);
            this.routerID = dec2.doFinal(encRouterID);

            Cipher dec3 = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec3 = new GCMParameterSpec(128, iv);
            dec3.init(Cipher.DECRYPT_MODE, sk, gcmSpec3);
            encMessage = dec3.doFinal(encMessage); // still in encrypted form

        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | InvalidAlgorithmParameterException e) {
            System.err.println("Error decrypting tunnelID: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Second stage of encryption using AES. This method encrypts the entire
     * EndpointPayload object using the secret key provided. This should only be
     * used
     * after the ElGamal encryption has been applied to the payload.
     * 
     * @param sk
     */
    public void layerEncrypt(SecretKey sk, byte[] iv) {
        // go item by item and encrypt, im reusing the same iv because i value self care
        try {
            Cipher enc1 = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec1 = new GCMParameterSpec(128, iv);
            enc1.init(Cipher.ENCRYPT_MODE, sk, gcmSpec1);
            encTunnelID = enc1.doFinal(encTunnelID);

            Cipher enc2 = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec2 = new GCMParameterSpec(128, iv);
            enc1.init(Cipher.ENCRYPT_MODE, sk, gcmSpec2);
            encRouterID = enc2.doFinal(encRouterID);

            // Encrypt the already encrypted payload using AES
            Cipher enc3 = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec3 = new GCMParameterSpec(128, iv);
            enc1.init(Cipher.ENCRYPT_MODE, sk, gcmSpec3);
            encMessage = enc3.doFinal(encMessage);

        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | InvalidAlgorithmParameterException e) {
            System.err.println("Error encrypting tunnelID: " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * This method decrypts the entire EndpointPayload object using the secret key
     * provided. This needs to be used at every hop.
     * 
     * @param sk
     */
    public void layerDecrypt(SecretKey sk, byte[] iv) {
        // Decrypt the tunnelID and routerID using ElGamal
        try {
            Cipher dec1 = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec1 = new GCMParameterSpec(128, iv);
            dec1.init(Cipher.DECRYPT_MODE, sk, gcmSpec1);
            encTunnelID = dec1.doFinal(encTunnelID);

            Cipher dec2 = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec2 = new GCMParameterSpec(128, iv);
            dec2.init(Cipher.DECRYPT_MODE, sk, gcmSpec2);
            encRouterID = dec2.doFinal(encRouterID);

            Cipher dec3 = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec3 = new GCMParameterSpec(128, iv);
            dec3.init(Cipher.DECRYPT_MODE, sk, gcmSpec3);
            encMessage = dec3.doFinal(encMessage);

        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | InvalidAlgorithmParameterException e) {
            System.err.println("Error decrypting tunnelID: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        if (!(arg0 instanceof JSONObject)) {
            throw new InvalidObjectException("Must be JSONObject");
        }
        // IF IT AINT BROKE DONT FIX IT!!!
        JSONObject jsonObject = (JSONObject) arg0;
        if (jsonObject.containsKey("encTunnelID")) {
            this.encTunnelID = Base64.decode(jsonObject.getString("encTunnelID"));
        }
        if (jsonObject.containsKey("encRouterID")) {
            this.encRouterID = Base64.decode(jsonObject.getString("encRouterID"));
        }
        if (jsonObject.containsKey("encMessage")) {
            this.encMessage = Base64.decode(jsonObject.getString("encMessage"));
        }
        if (jsonObject.containsKey("tunnelID")) {
            Object test = jsonObject.get("tunnelID");
            this.tunnelID = (Integer) test;
        }
        if (jsonObject.containsKey("routerID")) {
            this.routerID = Base64.decode(jsonObject.getString("routerID"));
        }
        if (jsonObject.containsKey("jsonObject")) {
            this.jsonObject = jsonObject.getObject("jsonObject");
        }

        // in case youre curious to see the trials and tribulations of this code...
        // jsonObject.checkValidity(new String[] { "tunnelID", "routerID", "payload" });
        // todo wtf why did this fix our issues - seth
        // bro shit just be happenin idk - sam
        // System.err.println(jsonObject.getFormattedJSON());
        // Object test = jsonObject.get("tunnelID"); // i loveeee i loveeeee
        // // System.out.println(test);
        // this.tunnelID = (Integer) test;
        // this.routerID = new RouterID(jsonObject.getObject("routerID"));
        // this.jsonObject = jsonObject.getObject("payload"); // pray this works
    }

    @Override
    public JSONObject toJSONType() {
        // TODO Auto-generated method stub
        JSONObject json = new JSONObject();
        if (encTunnelID != null) {
            json.put("encTunnelID", Base64.toBase64String(this.encTunnelID));
        }
        if (encRouterID != null) {
            json.put("encRouterID", Base64.toBase64String(this.encRouterID));
        }
        if (encMessage != null) {
            json.put("encMessage", Base64.toBase64String(this.encMessage));
        }

        json.put("jsonObject", this.jsonObject);

        if (tunnelID != -1) {
            json.put("tunnelID", this.tunnelID);
        }
        if (routerID != null) {
            json.put("routerID", Base64.toBase64String(this.routerID));
        }
        // System.out.println("EndpointPayload serializes to: " +
        // json.getFormattedJSON());
        return json;
    }

}
