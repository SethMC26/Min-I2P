package server;

import common.MessageSocket;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.util.Tuple;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidObjectException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    // ------ Private Variables ------
    private static String configFile = "test-data/config/config.json";
    private static boolean debug = false;
    private static int port;
    private static String databaseFile;
    private static String usersFile;
    private static String audioFile;
    private static AudioDatabase audioDatabase;
    private static UsersDatabase usersDatabase;

    /**
     * Prints the usage of the server
     */
    public static void usage() {
        System.out.println("Usage: ");
        System.out.println("  Server");
        System.out.println("  Server --help");
        System.out.println("  Server --config <config_file>");
        System.out.println("Options: ");
        System.out.printf("  %-15s %-20s\n", "-h, --help", "Show this help message");
        System.out.printf("  %-15s %-20s\n", "-c, --config", "Sets the config file for the server");
    }

    /**
     * Processes the command line arguments
     *
     * @param args The command line arguments
     */
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
        } catch (InvalidObjectException | FileNotFoundException e) {
            System.err.println("Error reading config file!!!");
            System.exit(1);
        }

        // Check to see if the database file exists
        log("Port: " + port);
        log("Database File: " + databaseFile);
        log("Users File: " + usersFile);
        log("Audio File: " + audioFile);

    }

    public static void main(String[] args) {
        BouncyCastleProvider c = new BouncyCastleProvider();
        if (args.length >= 5) {
            System.err.println("Not a valid input!");
            usage();
        }
        processArgs(args);

        usersDatabase = new UsersDatabase(usersFile);
        audioDatabase = new AudioDatabase(databaseFile, audioFile);

        serverStart();
    }

    /**
     * Deserializes the JSON object into the server variables
     *
     * @param jsonType The JSON object to deserialize
     * @throws InvalidObjectException If the JSON object is not a JSONObject or does not have the
     */
    private static void deserialize(JSONType jsonType) throws InvalidObjectException {
        // Check if the JSON object is a JSONObject
        if (!(jsonType instanceof JSONObject)) {
            throw new InvalidObjectException("JSONObject expected.");
        }

        JSONObject obj = (JSONObject) jsonType;

        // Check if the JSON object has the needed keys
        obj.checkValidity(new String[]{"port", "database-file", "users-file", "audio-file"});

        if (obj.containsKey("debug"))
            debug = obj.getBoolean("debug");

        // Set the variables to the values in the JSON object
        port = obj.getInt("port");
        databaseFile = obj.getString("database-file");
        usersFile = obj.getString("users-file");
        audioFile = obj.getString("audio-file");
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

    private static void serverStart() {
        ExecutorService executor = Executors.newFixedThreadPool(10);

        try {

            ServerSocket serverSocket = new ServerSocket(port);

            while(true) {

                MessageSocket socket = new MessageSocket(serverSocket.accept());

                executor.execute(new ServerConnectionHandler(socket, audioDatabase, usersDatabase));
            }
        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
        }

    }
}
