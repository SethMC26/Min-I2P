package client;

import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.util.Tuple;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Client {

    private static String userName;
    private static String songName;
    private static String songPath;
    private static String ipAddress;
    private static int port;

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

        if (doHelp) {
            usage();
            System.exit(0);
        }

        if (doCreate) {
            if (userName == null || port <= 0 || port >= 65535 || ipAddress == null) {
                System.out.println("Missing required arguments for create");
                usage();
                System.exit(1);
            }

            // Create the user in the database if the user does not already exist
            System.out.println("Creating user...");
        }

        if (doAdd) {
            if (userName == null || port <= 0 || port >= 65535 || ipAddress == null || songName == null || songPath == null) {
                System.out.println("Missing required arguments for add");
                usage();
                System.exit(1);
            }

            // Add the song to the database if the user exists and if the song does not already exist in the database
            System.out.println("Adding song...");
        }

        if (doPlay) {
            if (userName == null || port <= 0 || port >= 65535 || ipAddress == null || songName == null) {
                System.out.println("Missing required arguments for play");
                usage();
                System.exit(1);
            }

            // Play the song if the user exists and the song is in the database
            System.out.println("Playing song...");
        }

        if (doList) {
            if (userName == null || port <= 0 || port >= 65535 || ipAddress == null) {
                System.out.println("Missing required arguments for list");
                usage();
                System.exit(1);
            }

            // List all the songs in the database if the user exists
            System.out.println("Listing songs...");
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
}
