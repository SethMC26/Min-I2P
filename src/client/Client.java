package client;

import common.I2P.IDs.Destination;
import common.I2P.NetworkDB.Lease;
import common.I2P.NetworkDB.LeaseSet;
import common.I2P.router.Router;
import common.Logger;
import common.message.ByteMessage;
import common.message.Message;
import common.message.Request;
import common.message.Response;
import common.transport.I2CP.*;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.codec.Base32;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.util.Tuple;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {

    // -------- Private Variables -------- //
    public static InetAddress hostRouter;
    public static int routerPort;
    public static int servicePort ;
    public static InetSocketAddress bootstrapPeer;
    private static byte[] destHash;
    private static String clientHash;
    private static int sessionID;
    private static I2CPSocket socket;
    private static String configFile = "test-data/config/clientConfig.json";

    /**
     * This method is used to display the usage of the client
     */
    public static void usage() {
        System.out.println("Usage:");
        System.out.println("  client ");
        System.out.println("  client --help");
        System.out.println("  client --config <config_file>");
        System.out.println("Options:");
        System.out.printf("  %-15s %-20s\n", "-h, --help", "Display this help message");
        System.out.printf("  %-15s %-20s\n", "-c, --config", "The config file to use");
    }

    /**
     * This method processes the command line arguments
     *
     * @param args - String[] the command line arguments
     */
    public static void processArgs(String[] args) {
        OptionParser parser;

        boolean doHelp = false;

        LongOption[] opts = new LongOption[2];
        opts[0] = new LongOption("help", false, 'h');
        opts[1] = new LongOption("config", true, 'c');

        Tuple<Character, String> currOpt;

        parser = new OptionParser(args);
        parser.setLongOpts(opts);

        parser.setOptString("hc:");

        while (parser.getOptIdx() != args.length) {
            currOpt = parser.getOpt();
            switch (currOpt.getFirst()) {
                case 'h':
                    doHelp = true;
                    break;
                case 'c':
                    configFile = currOpt.getSecond();
                    break;
                default:
            }
        }

        // Check if the user requested help
        if (doHelp) {
            usage();
            System.exit(0);
        }

        // Check if the destination hash is provided
        if (destHash == null && configFile == null) {
            System.out.println("Destination hash is required");
            usage();
            System.exit(1);
        }

        // Since the config file is a path have to check to see if it exists
        File file = new File(configFile);
        if (!file.exists() || file.length() == 0) {
            System.err.println("No valid config file provided!!!");
            System.exit(1);
        }

        // Check to see if the config file can be read as a JSON object
        try {
            deserialize(JsonIO.readObject(file));
        } catch (InvalidObjectException | FileNotFoundException | UnknownHostException e) {
            System.err.println("Error reading config file!!!");
            System.exit(1);
        }

        // Start the client
        startClient();

    }

    public static void main(String[] args) {
        if (args.length > 3) {
            usage();
            System.exit(1);
        }
        processArgs(args);
    }

    /**
     * This method deserializes the JSON object and sets the variables
     *
     * @param jsonType - JSONType the JSON object to deserialize
     * @throws InvalidObjectException - if the JSON object is not valid
     * @throws UnknownHostException - if the host is not valid
     */
    private static void deserialize(JSONType jsonType) throws InvalidObjectException, UnknownHostException {
        // Check if the JSON object is a JSONObject
        if (!(jsonType instanceof JSONObject)) {
            throw new InvalidObjectException("JSONObject expected.");
        }

        JSONObject obj = (JSONObject) jsonType;

        // Check if the JSON object has the needed keys
        obj.checkValidity(new String[]{"serverhash", "host_BS", "port_BS", "host_router", "RSTPort", "CSTPort"});

        // Get the values from the JSON object
        destHash = Base64.decode(obj.getString("serverhash"));
        routerPort = obj.getInt("RSTPort");
        servicePort = obj.getInt("CSTPort");
        bootstrapPeer = new InetSocketAddress(obj.getString("host_BS"), obj.getInt("port_BS"));
        hostRouter = InetAddress.getByName(obj.getString("host_router"));

    }

    /**
     * This method starts the client and creates the router
     */
    private static void startClient() {
        try {
            // speciality floodfill router
            Security.addProvider(new BouncyCastleProvider()); // Add BouncyCastle provider for cryptography

            int numberOfRouters = 5; // Specify the number of routers to create
            Logger log = Logger.getInstance();
            log.setMinLevel(Logger.Level.ERROR);

            //start router
            Thread router = new Thread(new Router(hostRouter, routerPort, servicePort, bootstrapPeer));
            router.start();

            try {
                Thread.sleep(20000); //wait until router setup
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            KeyPair destEd25519Key = generateKeyPairEd();
            KeyPair destElgamalKey = generateKeyPairElGamal();

            Destination clientDest = new Destination(destEd25519Key.getPublic());

            //do client stuff
            socket = new I2CPSocket("127.0.0.1", servicePort);
            socket.sendMessage(new CreateSession(clientDest));

            I2CPMessage recvMessage;
            recvMessage = socket.getMessage();

            if (recvMessage.getType() != I2CPMessageTypes.SESSIONSTATUS) {
                System.err.println("bad type: " + recvMessage.getType());
                System.err.println(recvMessage.toJSONType().getFormattedJSON());
                return;
            }
            SessionStatus sessionStatus = (SessionStatus) recvMessage;

            if (sessionStatus.getStatus() != SessionStatus.Status.CREATED) {
                System.err.println("could not create session " + sessionStatus.getStatus());
                return;
            }

            //IMPORTANT NOTE this is the session ID since it is generated by the router
            sessionID = sessionStatus.getSessionID();
            recvMessage = socket.getMessage();

            if (recvMessage.getType() != I2CPMessageTypes.REQUESTLEASESET) {
                System.err.println("Bad type " + recvMessage.getType());
                System.err.println(recvMessage.toJSONType().getFormattedJSON());
            }

            RequestLeaseSet requestLeaseSet = (RequestLeaseSet) recvMessage;
            System.out.println("leases are" + requestLeaseSet.getLeases().size());
            ArrayList<Lease> leases = new ArrayList<>();
            leases.addAll(requestLeaseSet.getLeases());

            LeaseSet leaseSet = new LeaseSet(leases, clientDest, destElgamalKey.getPublic(), destEd25519Key.getPrivate());

            socket.sendMessage(new CreateLeaseSet(sessionID, destElgamalKey.getPrivate(), leaseSet));

            // Get the clients destination hash
            clientHash = Base64.toBase64String(clientDest.getHash());

            Destination currDest = null;

            socket.sendMessage(new DestinationLookup(sessionID, destHash));
            recvMessage = socket.getMessage();
            System.out.println("Got message from router");
            DestinationReply reply = (DestinationReply) recvMessage;

            if (reply.getDestination() != null) {
                currDest = reply.getDestination();
                System.out.println("Got destination! " + currDest);
                System.out.println("You can now send there!");
            } else {
                System.err.println("Destination not found :(");
                System.exit(1);
            }

            while (true) {
                // basic testing loop
                Scanner input = new Scanner(System.in);

                System.out.println("What would you like to do?");
                System.out.println("1. Create a user (1)");
                System.out.println("2. Add a song to the server (2)");
                System.out.println("3. Play a song from the server (3)");
                System.out.println("4. List all songs on the server (4)");

                System.out.print("Input: ");
                int usercase = input.nextInt();

                input.nextLine();

                switch (usercase) {
                    // Create a user
                    case 1 -> {
                        System.out.print("Please enter your username: ");
                        String username = input.nextLine();
                        String password = new String(System.console().readPassword("Enter Password: "));

                        Request request = new Request("Create", clientHash, username, password);
                        SendMessage msg = new SendMessage(sessionID, currDest, new byte[4], request.toJSONType());
                        socket.sendMessage(msg);

                        long startTime = System.currentTimeMillis();
                        while (!socket.hasMessage() && (System.currentTimeMillis() - startTime) > 5000) { // wait for 5 seconds
                            Thread.sleep(100);
                        }

                        if (System.currentTimeMillis() - startTime > 5000) {
                            System.out.println("Timeout waiting for response");
                            break;
                        }

                        recvMessage = socket.getMessage();

                        if (!(recvMessage instanceof PayloadMessage)) {
                            System.err.println("Error: expected PayloadMessage, got " + recvMessage.getType());
                            break;
                        }

                        PayloadMessage payloadMessage = (PayloadMessage) recvMessage;

                        Message recvMsg = new Message(payloadMessage.getPayload());

                        if (!recvMsg.getType().equals("Status")) {
                            System.err.println("Error: expected Status message, got " + recvMsg.getType());
                            break;
                        }

                        Response response = new Response(payloadMessage.getPayload());
                        boolean status = response.getStatus();

                        if (!status) {
                            System.out.println("User not added successfully");
                            break;
                        }

                        System.out.println("User added successfully");
                        System.out.println("TOTP secret: " + Base32.encodeToString(Base64.decode(response.getPayload()), true));

                    }

                    // Add a song to the server
                    case 2 -> {
                        if (!authenticateUser(input, currDest)) {
                            break;
                        }

                        System.out.print("Please enter the song name: ");
                        String songname = input.nextLine();
                        System.out.print("Please enter the songs file path: ");
                        String filePath = input.nextLine();

                        // Check if the file is a .wav file
                        if (!filePath.endsWith(".wav")) {
                            System.out.println("Error: File is not a .wav file");
                            break;
                        }

                        byte[] audioBytes;
                        try {
                            File file = new File(filePath);
                            if (!file.exists()) {
                                System.err.println("Error: File does not exist");
                                break;
                            }

                            Path path = file.toPath();
                            audioBytes = Files.readAllBytes(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        List<byte[]> chunks = chunkAudioData(audioBytes, 64); // 128 bytes per chunk

                        Request request = new Request("Add", clientHash, songname, chunks.size());
                        SendMessage msg = new SendMessage(sessionID, currDest, new byte[4], request.toJSONType());
                        socket.sendMessage(msg);

                        long startTime = System.currentTimeMillis();
                        while (!socket.hasMessage() && (System.currentTimeMillis() - startTime) > 5000) { // wait for 5 seconds
                            Thread.sleep(100);
                        }
                        if (System.currentTimeMillis() - startTime > 5000) {
                            System.out.println("Timeout waiting for response");
                            break;
                        }

                        recvMessage = socket.getMessage();
                        if (!(recvMessage instanceof PayloadMessage)) {
                            System.err.println("Error: expected PayloadMessage, got " + recvMessage.getType());
                            break;
                        }

                        PayloadMessage payloadMessage = (PayloadMessage) recvMessage;
                        Message recvMsg = new Message(payloadMessage.getPayload());

                        if (!recvMsg.getType().equals("Status")) {
                            System.err.println("Error: expected Status message, got " + recvMsg.getType());
                            break;
                        }

                        Response response = new Response(payloadMessage.getPayload());
                        boolean status = response.getStatus();

                        if (!status) {
                            System.out.println("Song can not be added");
                            break;
                        }

                        System.out.println("Song being added now");

                        sendSong(currDest, chunks);

                        System.out.println("Song successfully sent");

                    }

                    // Play a song from the server
                    case 3 -> {
                        if (!authenticateUser(input, currDest)) {
                            break;
                        }

                        System.out.print("Please enter the song name: ");
                        String songname = input.nextLine();

                        Request request = new Request("Play", clientHash, songname);
                        SendMessage msg = new SendMessage(sessionID, currDest, new byte[4], request.toJSONType());
                        socket.sendMessage(msg);

                        long startTime = System.currentTimeMillis();
                        while (!socket.hasMessage() && (System.currentTimeMillis() - startTime) > 5000) { // wait for 5 seconds
                            Thread.sleep(100);
                        }
                        if (System.currentTimeMillis() - startTime > 5000) {
                            System.out.println("Timeout waiting for response");
                            break;
                        }

                        recvMessage = socket.getMessage();

                        if (!(recvMessage instanceof PayloadMessage)) {
                            System.err.println("Error: expected PayloadMessage, got " + recvMessage.getType());
                            break;
                        }

                        PayloadMessage payloadMessage = (PayloadMessage) recvMessage;
                        Message recvMsg = new Message(payloadMessage.getPayload());

                        if (!recvMsg.getType().equals("Status")) {
                            System.err.println("Error: expected Status message, got " + recvMsg.getType());
                            break;
                        }

                        Response response = new Response(payloadMessage.getPayload());
                        boolean status = response.getStatus();

                        if (!status) {
                            System.out.println("Song can not be played");
                            break;
                        }

                        LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

                        Thread enqueueThread = new Thread(new EnqueueClient(queue, socket));
                        enqueueThread.start();

                        Thread dequeueThread = new Thread(new DequeueClient(queue));
                        dequeueThread.start();

                    }

                    // List all songs on the server
                    case 4 -> {
                        if (!authenticateUser(input, currDest)) {
                            break;
                        }

                        Request request = new Request("List", clientHash);
                        SendMessage msg = new SendMessage(sessionID, currDest, new byte[4], request.toJSONType());

                        socket.sendMessage(msg);

                        long startTime = System.currentTimeMillis();
                        while (!socket.hasMessage() && (System.currentTimeMillis() - startTime) > 5000) { // wait for 5 seconds
                            Thread.sleep(100);
                        }
                        if (System.currentTimeMillis() - startTime > 5000) {
                            System.out.println("Timeout waiting for response");
                            break;
                        }

                        recvMessage = socket.getMessage();

                        if (!(recvMessage instanceof PayloadMessage)) {
                            System.err.println("Error: expected PayloadMessage, got " + recvMessage.getType());
                            break;
                        }

                        PayloadMessage payloadMessage = (PayloadMessage) recvMessage;
                        Message recvMsg = new Message(payloadMessage.getPayload());

                        if (!recvMsg.getType().equals("Status")) {
                            System.err.println("Error: expected Status message, got " + recvMsg.getType());
                            break;
                        }

                        Response response = new Response(payloadMessage.getPayload());
                        boolean status = response.getStatus();

                        if (!status) {
                            System.out.println("Song can not be listed");
                            break;
                        }

                        System.out.println("Songs on the server: ");
                        String payload = response.getPayload();
                        if (payload != null) {
                            String[] songs = payload.split(",");
                            for (String song : songs) {
                                System.out.println(song);
                            }
                        } else {
                            System.out.println("No songs found");
                        }

                    }
                }

                System.out.println("Do you want to continue? (y/n)");
                String cont = input.nextLine();
                if (cont.equalsIgnoreCase("n")) {
                    System.out.println("Exiting...");
                    socket.sendMessage(new DestroySession(sessionID));
                    socket.close();
                    System.exit(0);
                }

            }

        } catch(IOException | InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Generates an Elgamal key pair for the router
     *
     * @return - KeyPair the key pair for the router
     */
    private static KeyPair generateKeyPairElGamal() {
        // Generate a key pair for the router
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ElGamal");
            keyGen.initialize(2048); // 2048 bits for RSA
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates an Ed25519 key pair for the router
     *
     * @return - KeyPair the key pair for the router
     */
    private static KeyPair generateKeyPairEd() {
        // Generate a key pair for the router
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("Ed25519", "BC");
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e); // should never hit case
        }
    }

    /**
     * This method authenticates the user with the server
     *
     * @param input - Scanner the input scanner
     * @param currDest - Destination the current destination
     * @return - boolean true if the user is authenticated, false otherwise
     * @throws InterruptedException - if the thread is interrupted
     * @throws IOException - if there is an error sending the message
     */
    private static boolean authenticateUser(Scanner input, Destination currDest) throws InterruptedException, IOException {
        System.out.println("Please authenticate with your username, password, and otp code; ");
        System.out.print("Username: ");
        String username = input.nextLine();
        String password = new String(System.console().readPassword("Enter Password: "));
        System.out.print("OTP code: ");
        int otpCode = input.nextInt();

        input.nextLine(); // consume the newline character

        Request request = new Request("Authenticate", clientHash, username, password, otpCode);
        SendMessage msg = new SendMessage(sessionID, currDest, new byte[4], request.toJSONType());
        socket.sendMessage(msg);

        long startTime = System.currentTimeMillis();
        while (!socket.hasMessage() && (System.currentTimeMillis() - startTime) > 5000) { // wait for 5 seconds
            Thread.sleep(100);
        }

        if (System.currentTimeMillis() - startTime > 5000) {
            System.out.println("Timeout waiting for response");
            return false;
        }

        I2CPMessage recvMessage = socket.getMessage();

        if (!(recvMessage instanceof PayloadMessage)) {
            System.err.println("Error: expected PayloadMessage, got " + recvMessage.getType());
            return false;
        }

        PayloadMessage payloadMessage = (PayloadMessage) recvMessage;

        Message recvMsg = new Message(payloadMessage.getPayload());

        if (!recvMsg.getType().equals("Status")) {
            System.err.println("Error: expected Status message, got " + recvMsg.getType());
            return false;
        }

        Response response = new Response(payloadMessage.getPayload());
        boolean status = response.getStatus();

        if (!status) {
            System.out.println("Authentication failed");
            return false;
        }

        System.out.println("Authentication successful");
        return true;
    }

    /**
     * This method sends the song to the server in chunks
     *
     * @param currDest - Destination the current destination
     * @param chunks - List of byte[] the chunks of the song
     * @throws IOException - if there is an error sending the message
     * @throws InterruptedException - if the thread is interrupted
     */
    private static void sendSong(Destination currDest, List<byte[]> chunks) throws IOException, InterruptedException {
        for (int id = 0; id < chunks.size(); id++) {
            byte[] chunk = chunks.get(id);
            // create a byte message
            ByteMessage bytes = new ByteMessage("Byte", clientHash, chunk, id);
            SendMessage msg = new SendMessage(sessionID, currDest, new byte[4], bytes.toJSONType());

            // Send the message 3 times
            socket.sendMessage(msg);
            Thread.sleep(5);

        }

        Thread.sleep(100);

        Message message = new Message("End", clientHash);
        SendMessage msg = new SendMessage(sessionID, currDest, new byte[4], message.toJSONType());
        socket.sendMessage(msg);

        System.out.println("Size of chunks: " + chunks.size());

    }

    /**
     * Splits the audio data into chunks of the specified size
     *
     * @param source - byte[] the audio data to be split
     * @param chunkSize - int the size of each chunk
     * @return - A list of byte arrays
     */
    private static List<byte[]> chunkAudioData(byte[] source, int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }

        List<byte[]> result = new ArrayList<>();
        int sourceLength = source.length;

        for (int startIndex = 0; startIndex < sourceLength; startIndex += chunkSize) {
            int endIndex = Math.min(sourceLength, startIndex + chunkSize);
            result.add(Arrays.copyOfRange(source, startIndex, endIndex));
        }

        return result;
    }
}
