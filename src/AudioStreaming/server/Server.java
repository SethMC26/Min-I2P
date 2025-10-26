package AudioStreaming.server;

import common.I2P.IDs.Destination;
import common.I2P.NetworkDB.Lease;
import common.I2P.NetworkDB.LeaseSet;
import common.I2P.router.Router;
import common.Logger;
import common.transport.I2CP.*;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.util.Tuple;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import AudioStreaming.server.databases.AudioDatabase;
import AudioStreaming.server.databases.UsersDatabase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {

    private static String configFile = "test-data/config/serverConfig.json";
    private static boolean debug = false;
    private static String databaseFile;
    private static String usersFile;
    private static String audioFile;
    private static AudioDatabase audioDatabase;
    private static UsersDatabase usersDatabase;

    static int routerPort;
    static int servicePort;
    static InetSocketAddress bootstrapPeer;
    static InetAddress hostRouter;

    private static PublicKey publicKey;
    private static PrivateKey privateKey;
    private static int sessionID;

    public static void usage() {
        System.out.println("Usage: ");
        System.out.println("  Server");
        System.out.println("  Server --help");
        System.out.println("  Server --config <config_file>");
        System.out.println("Options: ");
        System.out.printf("  %-15s %-20s\n", "-h, --help", "Show this help message");
        System.out.printf("  %-15s %-20s\n", "-c, --config", "Sets the config file for the AudioStreaming.server");
    }

    public static void processArgs(String[] args) {
        OptionParser parser;

        boolean doHelp = false;

        LongOption[] opts = new LongOption[2];
        opts[0] = new LongOption("config", true, 'c');
        opts[1] = new LongOption("help", false, 'h');

        Tuple<Character, String> currOpt;

        parser = new OptionParser(args);
        parser.setLongOpts(opts);

        parser.setOptString("c:h");

        while(parser.getOptIdx() != args.length) {

            currOpt = parser.getLongOpt(false);

            switch (currOpt.getFirst()) {
                case 'c':
                    configFile = currOpt.getSecond();
                    break;
                case 'h':
                    doHelp = true;
                    break;
                default:
            }
        }

        // Check if help was requested
        if (doHelp) {
            usage();
            System.exit(0);
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

        // Check to see if the database file exists

    }

    public static void main(String[] args) throws IOException {

        // ------- Setting up the AudioStreaming.server -------- //
        Security.addProvider(new BouncyCastleProvider());

        if (args.length >= 5) {
            System.err.println("Not a valid input!");
            usage();
        }
        processArgs(args);

        usersDatabase = new UsersDatabase(usersFile);
        audioDatabase = new AudioDatabase(databaseFile, audioFile);

        // ------- Starting the router -------- //
        int numberOfRouters = 5; // Specify the number of routers to create
        Logger log = Logger.getInstance();
        log.setMinLevel(Logger.Level.ERROR);

        System.out.println("Starting router...");
        //start router
        Thread router = new Thread(new Router(hostRouter,routerPort, servicePort, bootstrapPeer));
        router.start();
        try {
            Thread.sleep(100); // wait for tunnel to be created for a second
        } catch (InterruptedException e) {
            // this likely wont happen but if it does we will just ignore it
            log.warn("CST-CCH: Tunnel creation wait interrupted", e);
        }
        KeyPair destElgamalKey = generateKeyPairElGamal();

        Destination clientDest = new Destination(publicKey);

        //do AudioStreaming.client stuff
        I2CPSocket socket = new I2CPSocket("127.0.0.1", servicePort);
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

        LeaseSet leaseSet = new LeaseSet(leases, clientDest, destElgamalKey.getPublic(), privateKey);

        socket.sendMessage(new CreateLeaseSet(sessionID, destElgamalKey.getPrivate(), leaseSet));

        System.out.println("Router ready");
        // --------- Processing Messages --------- //

        LinkedBlockingQueue<ClientState> queue = new LinkedBlockingQueue<>();
        ConcurrentHashMap<String, ClientState> clients = new ConcurrentHashMap<>();

        // Start the AudioStreaming.server
        Thread enqueue = new Thread(new EnqueueServer(sessionID, clients, queue, socket));
        enqueue.start();

        Thread dequeue = new Thread(new DequeueServer(sessionID, clients, queue, socket, audioDatabase, usersDatabase));
        dequeue.start();

    }

    /**
     * Deserializes the JSON object into the AudioStreaming.server variables
     *
     * @param jsonType The JSON object to deserialize
     * @throws InvalidObjectException If the JSON object is not a JSONObject or does not have the
     */
    private static void deserialize(JSONType jsonType) throws InvalidObjectException, UnknownHostException {
        // Check if the JSON object is a JSONObject
        if (!(jsonType instanceof JSONObject)) {
            throw new InvalidObjectException("JSONObject expected.");
        }

        JSONObject obj = (JSONObject) jsonType;

        // Check if the JSON object has the needed keys
        obj.checkValidity(new String[]{"public", "private", "database-file", "users-file", "audio-file",
            "host_BS", "port_BS", "host_router", "RSTPort", "CSTPort"});

        if (obj.containsKey("debug"))
            debug = obj.getBoolean("debug");

        // Set the variables to the values in the JSON object
        databaseFile = obj.getString("database-file");
        usersFile = obj.getString("users-file");
        audioFile = obj.getString("audio-file");

        publicKey = getPublicKey(obj.getString("public"));
        privateKey = getPrivateKey(obj.getString("private"));

        routerPort = obj.getInt("RSTPort");
        servicePort = obj.getInt("CSTPort");
        bootstrapPeer = new InetSocketAddress(obj.getString("host_BS"), obj.getInt("port_BS"));
        hostRouter = InetAddress.getByName(obj.getString("host_router"));

    }

    /**
     * Prints the message if debug is enabled
     *
     * @param message The message to print
     */
    private static void log(String message) {
        if (debug) {
            System.out.println(message);
        }
    }

    /**
     * Gets the public key from the given string
     *
     * @param key - String of the public key
     * @return - PublicKey of the given string
     */
    private static PublicKey getPublicKey(String key) {
        byte[] keyBytes = Base64.decode(key);

        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("Ed25519", "BC");

            return kf.generatePublic(spec);
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the private key from the given string
     *
     * @param key - String of the private key
     * @return - PrivateKey of the given string
     */
    private static PrivateKey getPrivateKey(String key) {
        byte[] keyBytes = Base64.decode(key);

        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("Ed25519", "BC");

            return kf.generatePrivate(spec);
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Generates a key pair for the ElGamal encryption
     *
     * @return - KeyPair the generated key pair
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

}
