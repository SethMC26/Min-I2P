package server;

import common.MessageSocket;
import common.message.Message;
import common.message.Request;
import common.message.Response;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class ServerConnectionHandler implements Runnable {

    // ---------- Private Variables ---------- //
    private AudioDatabase audioDatabase;
    private UsersDatabase usersDatabase;
    private MessageSocket socket;

    ServerConnectionHandler(MessageSocket socket, AudioDatabase audioDatabase, UsersDatabase usersDatabase) {
        this.audioDatabase = audioDatabase;
        this.usersDatabase = usersDatabase;
        this.socket = socket;
    }

    @Override
    public void run() {
        Message recvMsg = socket.getMessage();
        switch (recvMsg.getType()) {
            case "Create":
                Request createRequest = (Request) recvMsg;

                // Create a new user
                String userName = createRequest.getUsername();
                String password = createRequest.getPassword();

                Response response;
                // check if user already exists\
                if (!usersDatabase.checkIfUserExists(userName)) {
                    try {
                        // Construct an key for the HMAC.
                        KeyGenerator hmacKeyGen = KeyGenerator.getInstance("HmacSHA1");
                        // create key to save as totp to use hmac later
                        SecretKey key = hmacKeyGen.generateKey();
                        String totp = Base64.toBase64String(key.getEncoded());

                        // add user
                        usersDatabase.addUser(userName, password, totp);

                        // send success message back to client
                        response = new Response("Status", true, totp);

                    } catch (NoSuchAlgorithmException e) {
                        // should not hit case
                        throw new RuntimeException(e);
                    }
                } else {
                    // send error message back to client
                    System.out.println("User already exists.");
                    response = new Response("Status", false, "User already exists.");
                }

                // send response to client
                socket.sendMessage(response);
                break;
            case "Authenticate":
                Request authenticate = (Request) recvMsg;

                if (!usersDatabase.checkIfUserExists(authenticate.getUsername())) {
                    System.err.println("User does not exist");
                    Response result = new Response("Status", false, "User does not exists.");
                    socket.sendMessage(result);
                    socket.close();
                    return;
                }

                if (!authProtocol(authenticate)) {
                    // auth failed, we should exit
                    socket.close();
                    return;
                }

                // keep connection open move on to next protocol post/get
                break;
            default:
                Response error = new Response("Status", false, "Unknown Message 1");
                socket.sendMessage(error);
                System.err.println("Unknown message type: " + recvMsg.getType());
                // close connection and return
                socket.close();
                return;
        }

        // Checking if there are still commands to process
        recvMsg = socket.getMessage();

        switch (recvMsg.getType()) {
            case "Add":
                Request addRequest = (Request) recvMsg;
                // Add a new song to the database
                String songName = addRequest.getSongname();

                // check if song already exists
                if (!audioDatabase.checkIfAudioExists(songName)) {
                    Response addResponse = new Response("Status", true, "Song can be added.");
                    socket.sendMessage(addResponse);
                } else {
                    Response addErrorResponse = new Response("Status", false, "Song already exists.");
                    socket.sendMessage(addErrorResponse);
                }
                break;
        }


    }

    private boolean authProtocol(Request authenticate) {
        Response result;
        User user = usersDatabase.getUserByName(authenticate.getUsername());
        //check password
        if (!user.checkPassoword(authenticate.getPassword())) {
            //if bad pass
            result = new Response("Status", false, "Authentication failed.");
            socket.sendMessage(result);
            socket.close();
            return false;
        }
        //check OTP
        try {
            // get T defined as (current Time - 0)/30
            Long T = System.currentTimeMillis() / 30000; // 30000ms = 30s
            ByteBuffer V = ByteBuffer.allocate(Long.BYTES);
            V.putLong(T);

            // Get a new HMAC instance.
            Mac hmac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.decode(user.getTotpKey()), "HmacSHA1");

            hmac.init(secretKeySpec);
            byte[] tag = hmac.doFinal(V.array());

            // copied from RFC 4226&5.4
            int offset = tag[19] & 0xf;
            int bin_code = (tag[offset] & 0x7f) << 24
                    | (tag[offset + 1] & 0xff) << 16
                    | (tag[offset + 2] & 0xff) << 8
                    | (tag[offset + 3] & 0xff);

            // mod by 10^6 to get a final number
            int otp = bin_code % 1_000_000;
            // create result based on if given OTP is correct
            if ((otp == authenticate.getOtp())) {
                result = new Response("Status", true, "");
                socket.sendMessage(result);
                return true;
            } else {
                result = new Response("Status", false, "Authentication failed.");
                socket.sendMessage(result);
                return false;
            }

        } catch (NumberFormatException e) {
            // catch if otp is not a number
            Response failmsg = new Response("Status", false, "Authentication failed.");
            socket.sendMessage(failmsg);
            socket.close();
            return false;
        } catch (NoSuchAlgorithmException e) {
            // should not hit case
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            // database key value is corrupt - not in scope of project
            throw new RuntimeException(e);
        }
    }


}
