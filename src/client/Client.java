package client;

import common.MessageSocket;
import common.message.ByteMessage;
import common.message.Message;
import common.message.Request;
import common.message.Response;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.codec.Base32;
import merrimackutil.util.Tuple;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {

    private static String userName;
    private static String songName;
    private static String songPath;
    private static String ipAddress;
    private static int port;

    /**
     * This tells the user how to use the program
     */
    public static void usage() {
        System.out.println("Usage: ");
        System.out.println("  client --create --userName <username> --port <port> --ipAddress <ipAddress>");
        System.out.println("  client --add --userName <username> --songName <songname> --songPath <songpath> --port <port> --ipAddress <ipAddress>");
        System.out.println("  client --play --userName <username> --songName <songname> --port <port> --ipAddress <ipAddress>");
        System.out.println("  client --list --userName <username> --port <port> --ipAddress <ipAddress>");
        System.out.println("  client --help");
        System.out.println("Options: ");
        System.out.printf("  %-15s %-20s\n", "-c, --create", "Create a new user");
        System.out.printf("  %-15s %-20s\n", "-u, --userName", "Username of the user");
        System.out.printf("  %-15s %-20s\n", "-p, --port", "Port number to connect to");
        System.out.printf("  %-15s %-20s\n", "-i, --ipAddress", "Host name to connect to");
        System.out.printf("  %-15s %-20s\n", "-a, --add", "Add a new song to the database");
        System.out.printf("  %-15s %-20s\n", "-n, --songName", "Name of the song");
        System.out.printf("  %-15s %-20s\n", "-s, --songPath", "File path to the song");
        System.out.printf("  %-15s %-20s\n", "-o, --play", "Play a song from the database");
        System.out.printf("  %-15s %-20s\n", "-l, --list", "List all the songs in the database");
        System.out.printf("  %-15s %-20s\n", "-h, --help", "Pulls up the help menus");
    }

    /**
     * Processes the arguments passed to the program
     *
     * @param args
     */
    public static void processArgs(String[] args) {
        OptionParser parser;

        boolean doHelp = false;
        boolean doCreate = false;
        boolean doAdd = false;
        boolean doPlay = false;
        boolean doList = false;

        LongOption[] opts = new LongOption[10];
        opts[0] = new LongOption("create", false, 'c');
        opts[1] = new LongOption("userName", true, 'u');
        opts[2] = new LongOption("port", true, 'p');
        opts[3] = new LongOption("ipAddress", true, 'i');
        opts[4] = new LongOption("add", false, 'a');
        opts[5] = new LongOption("songName", true, 'n');
        opts[6] = new LongOption("songPath", true, 's');
        opts[7] = new LongOption("play", false, 'o');
        opts[8] = new LongOption("list", false, 'l');
        opts[9] = new LongOption("help", false, 'h');

        Tuple<Character, String> currOpt;

        parser = new OptionParser(args);
        parser.setLongOpts(opts);

        parser.setOptString("cu:p:i:an:s:olh");

        while (parser.getOptIdx() != args.length) {
            currOpt = parser.getOpt();
            switch (currOpt.getFirst()) {
                case 'c':
                    doCreate = true;
                    break;
                case 'u':
                    userName = currOpt.getSecond();
                    break;
                case 'p':
                    try {
                        port = Integer.parseInt(currOpt.getSecond());
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid port number");
                        System.exit(1);
                    }
                    break;
                case 'i':
                    ipAddress = currOpt.getSecond();
                    break;
                case 'a':
                    doAdd = true;
                    break;
                case 'n':
                    songName = currOpt.getSecond();
                    break;
                case 's':
                    songPath = currOpt.getSecond();
                    break;
                case 'o':
                    doPlay = true;
                    break;
                case 'l':
                    doList = true;
                    break;
                case 'h':
                    doHelp = true;
                    break;
            }
        }

        // Check if help was requested
        if (doHelp) {
            usage();
            System.exit(0);
        }

        // Check if create was requested
        if (doCreate) {
            if (userName == null || port <= 0 || port >= 65535 || ipAddress == null) {
                System.out.println("Missing required arguments for create");
                usage();
                System.exit(1);
            }

            // Create the user in the database if the user does not already exist
            System.out.println("Creating user...");

            MessageSocket socket = null;
            try {
                socket = new MessageSocket(ipAddress, port);
            } catch (Exception e) {
                System.out.println("Error creating user: " + e.getMessage());
                System.exit(1);
            }

            String password = new String(System.console().readPassword("Enter password: "));

            Request createRequest = new Request("Create", userName, password);

            socket.sendMessage(createRequest);

            Message recvMsg = socket.getMessage();

            if (!recvMsg.getType().equals("Status")) {
                System.err.println("Error: Invalid response type: " + recvMsg.getType());
                socket.close();
                System.exit(1);
            }

            Response response = (Response) recvMsg;

            boolean status = response.getStatus();
            String payload = response.getPayload();

            if (status) {
                // spec says we should send key totp as base64 but display it as base 32
                String totp = Base32.encodeToString(Base64.decode(payload), true);
                System.out.println("Base 32 Key: " + totp);
            } else {
                System.err.println("\u001B[31mUser creation failed. Reason:\u001B[0m");
                System.out.println(payload);
            }

            socket.close();
        }

        // Check if add was requested
        if (doAdd) {
            if (userName == null || port <= 0 || port >= 65535 || ipAddress == null || songName == null || songPath == null) {
                System.out.println("Missing required arguments for add");
                usage();
                System.exit(1);
            }

            // Add the song to the database if the user exists and if the song does not already exist in the database
            System.out.println("Adding song...");

            MessageSocket socket = null;
            try {
                socket = new MessageSocket(ipAddress, port);
            } catch (IOException e) {
                System.err.println("Could not set up connection: " + e.getMessage());
                System.exit(1);
            }

            authenticate(socket);

            System.out.println("Authenticated");

            // --------- Send the Add Request --------- //
            Request addRequest = new Request("Add", userName, songName);

            socket.sendMessage(addRequest);

            Message recvMsg = socket.getMessage();

            if (!recvMsg.getType().equals("Status")) {
                System.err.println("Error: Invalid response type: " + recvMsg.getType());
                socket.close();
                System.exit(1);
            }

            Response response = (Response) recvMsg;

            boolean status = response.getStatus();

            if(!status) {
                System.err.println("Error: " + response.getPayload());
                socket.close();
                System.exit(1);
            }

            // --------- Send the Song Data --------- //

            // Checks to see if the file path given is a wav file
            String[] parts = songPath.split("\\.");
            if (parts.length < 2 || !parts[parts.length - 1].equals("wav")) {
                System.err.println("Error: Invalid file path or file type. Must be a .wav file");
                socket.close();
                System.exit(1);
            }

            // Checks if the file exists and gets the bytes if it does
            byte[] audioBytes;
            try {
                File file = new File(songPath);
                if (!file.exists()) {
                    System.err.println("Error: File does not exist");
                    socket.close();
                    System.exit(1);
                }

                Path path = file.toPath();
                audioBytes = Files.readAllBytes(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Send the song data to the server

            List<byte[]> chunks = chunkAudioData(audioBytes, 256); // Chunk size of 512 bytes

            System.out.println("Size of audio data: " + chunks.size());

            for (int i = 0; i < chunks.size(); i++) {
                byte[] chunk = chunks.get(i);
                ByteMessage byteMessage = new ByteMessage("Byte", chunk);
                socket.sendMessage(byteMessage);
            }

            socket.sendMessage(new Message("End")); // Send end message to indicate end of audio data

            recvMsg = socket.getMessage();

            if (!recvMsg.getType().equals("Status")) {
                System.err.println("Error: Invalid response type: " + recvMsg.getType());
                socket.close();
                System.exit(1);
            }

            response = (Response) recvMsg;

            status = response.getStatus();
            String payload = response.getPayload();

            if (status) {
                System.out.println("Song added successfully");
            } else {
                System.err.println("Error: " + payload);
            }

        }

        // Check if play was requested
        if (doPlay) {
            if (userName == null || port <= 0 || port >= 65535 || ipAddress == null || songName == null) {
                System.out.println("Missing required arguments for play");
                usage();
                System.exit(1);
            }

            // Play the song if the user exists and the song is in the database
            System.out.println("Playing song...");

            MessageSocket socket = null;
            try {
                socket = new MessageSocket(ipAddress, port);
            } catch (IOException e) {
                System.err.println("Could not set up connection: " + e.getMessage());
                System.exit(1);
            }

            authenticate(socket);
            System.out.println("Authenticated");

            // --------- Send the Play Request --------- //
            Request addRequest = new Request("Play", userName, songName);

            socket.sendMessage(addRequest);

            // TODO: Added the plaing music threads here

            LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

            EnqueueClient enqueueThread = new EnqueueClient(queue, socket);
            Thread enqueue = new Thread(enqueueThread);
            enqueue.start();

            DequeueClient dequeueThread = new DequeueClient(queue);
            Thread dequeue = new Thread(dequeueThread);
            dequeue.start();
        }

        // Check if list was requested
        if (doList) {
            if (userName == null || port <= 0 || port >= 65535 || ipAddress == null) {
                System.out.println("Missing required arguments for list");
                usage();
                System.exit(1);
            }

            // List all the songs in the database if the user exists
            System.out.println("Listing songs...");

            MessageSocket socket = null;
            try {
                socket = new MessageSocket(ipAddress, port);
            } catch (IOException e) {
                System.err.println("Could not set up connection: " + e.getMessage());
                System.exit(1);
            }

            authenticate(socket);
            System.out.println("Authenticated");

            // --------- Send the List Request --------- //
            Request addRequest = new Request("List", userName, songName);

            socket.sendMessage(addRequest);

            Message recvMsg = socket.getMessage();

            if (!recvMsg.getType().equals("Status")) {
                System.err.println("Error: Invalid response type: " + recvMsg.getType());
                socket.close();
                System.exit(1);
            }

            Response response = (Response) recvMsg;

            boolean status = response.getStatus();

            if(!status) {
                System.err.println("Error: " + response.getPayload());
                socket.close();
                System.exit(1);
            }

            // --------- Print the List of Songs --------- //

            String payload = response.getPayload();

            System.out.println("\nSongs in database:");
            String[] songs = payload.split(",");
            int i = 1;
            for (String song : songs) {
                System.out.println("   " + i + ": " + song);
                i++;
            }

        }
    }

    public static void main(String[] args) {
        BouncyCastleProvider c = new BouncyCastleProvider();
        if (args.length == 0) {
            usage();
            System.exit(0);
        }
        if (args.length > 11) {
            System.out.println("Invalid arguments");
            usage();
            System.exit(1);
        }
        processArgs(args);
    }

    // ---------- Private Methods ---------- //

    // Add any private methods here if needed
    private static void authenticate(MessageSocket sock) {
        // We might want to move this password string if post/get needs it as well -Seth
        String password = new String(System.console().readPassword("Enter Password: "));
        Scanner in = new Scanner(System.in);
        System.out.print("Enter OTP: ");
        Integer otp = in.nextInt();

        Request request = new Request("Authenticate", userName, password, otp);

        sock.sendMessage(request);

        Message response = sock.getMessage();
        if (!response.getType().equals("Status")) {
            sock.close();
            System.err.println("Got bad message type from server: " + response.getType());
            System.exit(1);
        }

        Response status = (Response) response;
        // if we get a bad status let's exit
        if (!status.getStatus()) {
            System.err.println(status.getPayload());
            sock.close();
            System.exit(1);
        }
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
