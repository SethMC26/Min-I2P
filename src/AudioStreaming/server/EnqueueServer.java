package AudioStreaming.server;

import common.I2P.IDs.Destination;
import AudioStreaming.message.ByteMessage;
import AudioStreaming.message.Message;
import AudioStreaming.message.Request;
import common.transport.I2CP.*;
import merrimackutil.json.types.JSONObject;
import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class EnqueueServer implements Runnable {

    private final int SESSION_ID;
    private final ConcurrentHashMap<String, ClientState> MAP;
    private final LinkedBlockingQueue<ClientState> QUEUE;
    private final I2CPSocket SOCKET;

    public EnqueueServer(int sessionID, ConcurrentHashMap<String, ClientState> map, LinkedBlockingQueue<ClientState> queue, I2CPSocket socket) {
        this.SESSION_ID = sessionID;
        this.MAP = map;
        this.QUEUE = queue;
        this.SOCKET = socket;
    }

    @Override
    public void run() {

        while (true) {

            // Checks if there is a message in the socket
            try {
                // Get the message from the socket
                I2CPMessage message = SOCKET.getMessage();

                // Check if the message is a PayloadMessage
                if (!(message instanceof PayloadMessage)) {
                    System.err.println("Error: Message is not a PayloadMessage");
                    continue;
                }
                PayloadMessage payloadMessage = (PayloadMessage) message;

                // Get the payload from the message
                JSONObject json = payloadMessage.getPayload();
                if (json == null) {
                    System.err.println("Error: Payload is null");
                    continue;
                }
                Message msg = new Message(json);

                // Get the destination hash from the message
                String destHash = msg.getDestHash();
                Destination dest;

                // Check if the destination hash is valid and save it to dest
                if (!MAP.containsKey(destHash)) {
                    // Gets the destination from the message
                    byte[] destHashBytes = Base64.decode(destHash);

                    // Sends a DestinationLookup message to the socket
                    SOCKET.sendMessage(new DestinationLookup(SESSION_ID, destHashBytes));

                    // Waits for the message to be received
                    I2CPMessage recvMessage = SOCKET.getMessage();

                    // Check if the message is a DestinationReply
                    DestinationReply reply = (DestinationReply) recvMessage;

                    // Check if the destination is null
                    if (reply.getDestination() != null) {
                        dest = reply.getDestination();
                        String destHashString = Base64.toBase64String(dest.getHash());

                        MAP.put(destHashString, new ClientState(dest, CommandType.NONE));
                    } else {
                        System.err.println("Error: Destination not found");
                        continue;
                    }
                } else {
                    dest = MAP.get(destHash).getClientDest();
                }

                switch (msg.getType()) {

                    // The user is requesting to create a new account
                    case "Create" -> {
                        Request request = new Request(json);

                        // Get the needed data from the message
                        String username = request.getUsername();
                        String password = request.getPassword();

                        ClientState client;

                        // Check if the AudioStreaming.client is already in the map
                        if (MAP.containsKey(destHash)) {
                            // Gets the AudioStreaming.client from the map and cleans it
                            ClientState existingClient = MAP.get(destHash);
                            existingClient.clean();

                            // Sets the AudioStreaming.client name and password
                            existingClient.setClientName(username);
                            existingClient.setClientPassword(password);
                            existingClient.setCommandType(CommandType.CREATE);

                            client = existingClient;
                        } else {
                            // Creates a new AudioStreaming.client and sets the name and password
                            client = new ClientState(dest, CommandType.CREATE);
                            client.setClientName(username);
                            client.setClientPassword(password);

                            MAP.put(destHash, client);
                        }

                        QUEUE.add(client);
                    }

                    // The user is requesting to authenticate
                    case "Authenticate" -> {
                        Request request = new Request(json);

                        // Get the needed data from the message
                        String username = request.getUsername();
                        String password = request.getPassword();
                        int otp = request.getOtp();

                        ClientState client;

                        // Check if the AudioStreaming.client is already in the map
                        if (MAP.containsKey(destHash)) {
                            // Gets the AudioStreaming.client from the map and cleans it
                            ClientState existingClient = MAP.get(destHash);
                            existingClient.clean();

                            // Sets the AudioStreaming.client name, password and otp
                            existingClient.setClientName(username);
                            existingClient.setClientPassword(password);
                            existingClient.setClientOTP(otp);
                            existingClient.setCommandType(CommandType.AUTHENTICATE);

                            client = existingClient;
                        } else {
                            // Creates a new AudioStreaming.client and sets the name, password and otp
                            client = new ClientState(dest, CommandType.AUTHENTICATE);
                            client.setClientName(username);
                            client.setClientPassword(password);
                            client.setClientOTP(otp);

                            MAP.put(destHash, client);
                        }
                        QUEUE.add(client);

                    }

                    // The user is requesting to add a song
                    case "Add" -> {
                        Request request = new Request(json);

                        // Get the needed data from the message
                        String songName = request.getSongname();
                        int songSize = request.getSize();

                        ClientState client;

                        if (MAP.containsKey(destHash)) {
                            // Gets the AudioStreaming.client from the map and cleans it
                            ClientState existingClient = MAP.get(destHash);
                            existingClient.clean();

                            // Sets the song name and size
                            existingClient.setSongname(songName);
                            existingClient.setSongSize(songSize);
                            existingClient.setCommandType(CommandType.ADD);

                            client = existingClient;
                        } else {
                            // Creates a new AudioStreaming.client and sets the song name and size
                            client = new ClientState(dest, CommandType.ADD);
                            client.setSongname(songName);
                            client.setSongSize(songSize);

                            MAP.put(destHash, client);
                        }
                        QUEUE.add(client);
                    }

                    // The user is requesting to play a song
                    case "Play" -> {
                        Request request = new Request(json);

                        // Get the needed data from the message
                        String songName = request.getSongname();

                        ClientState client;

                        // Check if the AudioStreaming.client is already in the map
                        if (MAP.containsKey(destHash)) {
                            // Gets the AudioStreaming.client from the map and cleans it
                            ClientState existingClient = MAP.get(destHash);
                            existingClient.clean();

                            // Sets the song name
                            existingClient.setSongname(songName);
                            existingClient.setCommandType(CommandType.PLAY);

                            client = existingClient;
                        } else {
                            // Creates a new AudioStreaming.client and sets the song name
                            client = new ClientState(dest, CommandType.PLAY);
                            client.setSongname(songName);

                            MAP.put(destHash, client);
                        }
                        QUEUE.add(client);
                    }

                    // The user is requesting the list of songs
                    case "List" -> {
                        ClientState client;

                        // Check if the AudioStreaming.client is already in the map
                        if (MAP.containsKey(destHash)) {
                            // Gets the AudioStreaming.client from the map and cleans it
                            ClientState existingClient = MAP.get(destHash);
                            existingClient.clean();

                            // Sets the song name
                            existingClient.setCommandType(CommandType.LIST);

                            client = existingClient;
                        } else {
                            // Creates a new AudioStreaming.client and sets the song name
                            client = new ClientState(dest, CommandType.LIST);

                            MAP.put(destHash, client);
                        }
                        QUEUE.add(client);
                    }

                    // The user is sending the song bytes
                    case "Byte" -> {
                        ByteMessage request = new ByteMessage(json);

                        // Get the needed data from the message
                        byte[] songData = request.getData();
                        int id = request.getId();

                        ClientState client;

                        client = new ClientState(dest, CommandType.SENDING);
                        client.setSongData(songData);
                        client.setByteID(id);

                        boolean isClientAuth = MAP.get(destHash).isAuthenticated();

                        client.setAuthenticated(isClientAuth);

                        QUEUE.add(client);

                    }

                    // End of the user sending the song bytes
                    case "End" -> {
                        System.out.println("EnqueueServer: End of song");

                        ClientState client;

                        // Check if the AudioStreaming.client is already in the map
                        if (MAP.containsKey(destHash)) {
                            ClientState existingClient = MAP.get(destHash);
                            existingClient.cleanSongData();

                            existingClient.setCommandType(CommandType.END);

                            client = existingClient;
                        } else {
                            client = new ClientState(dest, CommandType.END);

                            MAP.put(destHash, client);
                        }
                        System.out.println("EnqueueServer: Before adding to queue: " + QUEUE.size());
                        QUEUE.add(client);
                        System.out.println("EnqueueServer: Added AudioStreaming.client to queue: " + QUEUE.size());
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
