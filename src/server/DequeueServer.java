package server;

import common.I2P.IDs.Destination;
import common.message.ByteMessage;
import common.message.Response;
import common.transport.I2CP.I2CPSocket;
import common.transport.I2CP.SendMessage;
import org.bouncycastle.util.encoders.Base32;
import org.bouncycastle.util.encoders.Base64;
import server.databases.AudioDatabase;
import server.databases.User;
import server.databases.UsersDatabase;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class DequeueServer implements Runnable {

    // -------- Private Variables -------- //
    private final int SESSION_ID;
    private final ConcurrentHashMap<String, ClientState> MAP;
    private final LinkedBlockingQueue<ClientState> QUEUE;
    private final I2CPSocket SOCKET;
    private final AudioDatabase AUDIO_DATABASE;
    private final UsersDatabase USERS_DATABASE;
    private final HashMap<Destination, List<byte[]>> AUDIO_DATA_MAP;

    public DequeueServer(int sessionID, ConcurrentHashMap<String, ClientState> map, LinkedBlockingQueue<ClientState> queue,
                         I2CPSocket socket, AudioDatabase audioDatabase, UsersDatabase usersDatabase) {
        this.SESSION_ID = sessionID;
        this.MAP = map;
        this.QUEUE = queue;
        this.SOCKET = socket;
        this.AUDIO_DATABASE = audioDatabase;
        this.USERS_DATABASE = usersDatabase;
        this.AUDIO_DATA_MAP = new HashMap<>();
    }

    @Override
    public void run() {

        while (true) {
            try {
                ClientState clientState = QUEUE.take();

                // Process the client state
                CommandType commandType = clientState.getCommandType();

                switch (commandType) {
                    case CREATE -> {
                        // Get the client name and password from the client state
                        String clientNameCreate = clientState.getClientName();
                        String clientPasswordCreate = clientState.getClientPassword();

                        // Check if the user already exists in the database
                        if (USERS_DATABASE.checkIfUserExists(clientNameCreate)) {
                            System.out.println("User already exists");
                            Response response = new Response("Status", "", false, "User already exists");
                            SendMessage send = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], response.toJSONType());
                            SOCKET.sendMessage(send);
                            break;
                        }

                        // Generate a TOTP key
                        KeyGenerator hmacKeyGen = KeyGenerator.getInstance("HmacSHA1");
                        SecretKey key = hmacKeyGen.generateKey();
                        String totp = Base64.toBase64String(key.getEncoded());

                        // Store the user in the database
                        USERS_DATABASE.addUser(clientNameCreate, clientPasswordCreate, totp);

                        // Store the client state in the map
                        Response response = new Response("Status", "", true, totp);
                        SendMessage send = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], response.toJSONType());

                        // Send the response to the client
                        SOCKET.sendMessage(send);
                    }
                    case AUTHENTICATE -> {
                        // Get the client name, password, and OTP from the client state
                        String clientNameAuth = clientState.getClientName();
                        String clientPasswordAuth = clientState.getClientPassword();
                        int clientOTPAuth = clientState.getClientOTP();

                        // Check if the user exists in the database
                        if (!USERS_DATABASE.checkIfUserExists(clientNameAuth)) {
                            System.out.println("User does not exist");
                            Response responseAuth = new Response("Status", "", false, "User does not exist");
                            SendMessage sendAuth = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responseAuth.toJSONType());
                            SOCKET.sendMessage(sendAuth);
                            break;
                        }

                        // Check if the user is already authenticated
                        if (!authProtocol(clientNameAuth, clientPasswordAuth, clientOTPAuth)) {
                            System.out.println("Authentication failed");
                            Response responseAuthFail = new Response("Status", "", false, "Authentication failed");
                            SendMessage sendAuthFail = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responseAuthFail.toJSONType());
                            SOCKET.sendMessage(sendAuthFail);
                            break;
                        }

                        // Authentication successful
                        clientState.setAuthenticated(true);

                        // Store the client state in the map
                        System.out.println("Authentication successful");
                        Response responseAuthSuccess = new Response("Status", "", true, "Authentication successful");
                        SendMessage sendAuthSuccess = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responseAuthSuccess.toJSONType());

                        SOCKET.sendMessage(sendAuthSuccess);

                    }
                    case ADD -> {
                        // Check if the user is authenticated
                        if (!clientState.isAuthenticated()) {
                            System.out.println("User not authenticated");
                            Response responseAddFail = new Response("Status", "", false, "User not authenticated");
                            SendMessage sendAddFail = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responseAddFail.toJSONType());
                            SOCKET.sendMessage(sendAddFail);
                            break;
                        }

                        // Get the song name and size from the client state
                        String clientSongNameAdd = clientState.getSongname();
                        int clientSongSizeAdd = clientState.getSongSize();

                        // Check if the audio data exists in the database
                        if (AUDIO_DATABASE.checkIfAudioExists(clientSongNameAdd)) {
                            System.out.println("Audio already exist");
                            Response responseAdd = new Response("Status", "", false, "Audio already exist");
                            SendMessage sendAdd = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responseAdd.toJSONType());
                            SOCKET.sendMessage(sendAdd);
                            break;
                        }

                        // Check if the audio data is already present
                        Response responseAddSuccess = new Response("Status", "", true, "Audio can be added to the database");
                        SendMessage sendAddSuccess = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responseAddSuccess.toJSONType());
                        SOCKET.sendMessage(sendAddSuccess);

                        List<byte[]> list = new ArrayList<>();
                        for (int i = 0; i < clientSongSizeAdd; i++) {
                            list.add(null);
                        }

                        // Add the audio data to the map
                        AUDIO_DATA_MAP.put(clientState.getClientDest(), list);

                    }
                    case SENDING -> {
                        System.out.println("Dequeue: Sending command received");
                        // Check if the user is authenticated
                        if (!clientState.isAuthenticated()) {
                            System.out.println("User not authenticated");
                            Response responseSendingFail = new Response("Status", "", false, "User not authenticated");
                            SendMessage sendSendingFail = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responseSendingFail.toJSONType());
                            SOCKET.sendMessage(sendSendingFail);
                            break;
                        }

                        // Get the song data and byte ID from the client state
                        byte[] clientSongData = clientState.getSongData();
                        int clientByteID = clientState.getByteID();

                        Destination destSending = clientState.getClientDest();

                        // Check if the audio data exists in the map
                        if (!AUDIO_DATA_MAP.containsKey(destSending)) {
                            System.out.println("Audio data not found");
                            Response responseSendingFail = new Response("Status", "", false, "Audio data not found");
                            SendMessage sendSendingFail = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responseSendingFail.toJSONType());
                            SOCKET.sendMessage(sendSendingFail);
                            break;
                        }

                        // Get the audio data from the map
                        List<byte[]> audioDataSending = AUDIO_DATA_MAP.get(destSending);

                        // Check if the audio data is already present
                        if (audioDataSending.get(clientByteID) == null) {
                            audioDataSending.set(clientByteID, clientSongData);
                        }

                    }
                    case END -> {
                        System.out.println("Dequeue: End command received");

                        // Check if the user is authenticated
                        if (!clientState.isAuthenticated()) {
                            System.out.println("User not authenticated");
                            Response responseEndFail = new Response("Status", "", false, "User not authenticated");
                            SendMessage sendEndFail = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responseEndFail.toJSONType());
                            SOCKET.sendMessage(sendEndFail);
                            break;
                        }

                        Destination destEnd = clientState.getClientDest();

                        // Check if the audio data exists in the map
                        if (!AUDIO_DATA_MAP.containsKey(destEnd)) {
                            System.out.println("Audio data not found");
                            Response responseEndFail = new Response("Status", "", false, "Audio data not found");
                            SendMessage sendEndFail = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responseEndFail.toJSONType());
                            SOCKET.sendMessage(sendEndFail);
                            break;
                        }

                        List<byte[]> audioData = AUDIO_DATA_MAP.get(destEnd);

                        System.out.println("Audio data before adding to database: " + audioData.size());

                        AUDIO_DATABASE.addAudio(clientState.getSongname(), audioData, clientState.getSongSize());
                        MAP.remove(Base64.toBase64String(clientState.getClientDest().getHash()));
                        AUDIO_DATA_MAP.remove(destEnd);

                        System.out.println("Audio data added to the database");

                    }
                    case PLAY -> {
                        // Check if the user is authenticated
                        if (!clientState.isAuthenticated()) {
                            System.out.println("User not authenticated");
                            Response responsePlayFail = new Response("Status", "", false, "User not authenticated");
                            SendMessage sendPlayFail = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responsePlayFail.toJSONType());
                            SOCKET.sendMessage(sendPlayFail);
                            break;
                        }

                        String clientSongNamePlay = clientState.getSongname();

                        // Check if the audio exists in the database
                        if (!AUDIO_DATABASE.checkIfAudioExists(clientSongNamePlay)) {
                            System.out.println("Audio does not exist");
                            Response responsePlayFail = new Response("Status", "", false, "Audio does not exist");
                            SendMessage sendPlayFail = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responsePlayFail.toJSONType());
                            SOCKET.sendMessage(sendPlayFail);
                            break;
                        }

                        Response responsePlayFail = new Response("Status", "", true, "Audio does exist");
                        SendMessage sendPlayFail = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responsePlayFail.toJSONType());
                        SOCKET.sendMessage(sendPlayFail);

                        Thread.sleep(100);

                        // Get the audio data from the database
                        List<byte[]> audioDataPlay = AUDIO_DATABASE.getAudio(clientSongNamePlay);

                        Thread sendPlayedSongThread = new Thread(new SendPlayedSong(SESSION_ID, SOCKET, clientState, audioDataPlay));
                        sendPlayedSongThread.start();

                        String clientDest = Base64.toBase64String(clientState.getClientDest().getHash());

                        MAP.remove(clientDest);

                    }
                    case LIST -> {
                        // Check if the user is authenticated
                        if (!clientState.isAuthenticated()) {
                            System.out.println("User not authenticated");
                            Response responseListFail = new Response("Status", "", false, "User not authenticated");
                            SendMessage sendListFail = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responseListFail.toJSONType());
                            SOCKET.sendMessage(sendListFail);
                            break;
                        }

                        // List all audio in the database
                        String audioList = AUDIO_DATABASE.listAudio();

                        // Builds the messsages that needs to be sent
                        Response responseList = new Response("Status", "", true, audioList);
                        SendMessage sendList = new SendMessage(SESSION_ID, clientState.getClientDest(), new byte[4], responseList.toJSONType());

                        SOCKET.sendMessage(sendList);

                    }
                }

            } catch (InterruptedException | NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private boolean authProtocol(String username, String password, int totp) {
        //check password
        User user = USERS_DATABASE.getUser(username);

        if (!user.checkPassoword(password)) {
            //if bad pass
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
            return otp == totp;

        } catch (NumberFormatException e) {
            // catch if otp is not a number
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
