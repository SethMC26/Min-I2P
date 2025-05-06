package RelayPeer;

import common.I2P.router.Router;
import common.I2P.router.RouterConfig;
import common.Logger;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.util.Tuple;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.IOException;
import java.security.Security;

public class RelayPeer {
    public static void main(String[] args) throws IOException {
        // speciality floodfill router
        Security.addProvider(new BouncyCastleProvider());

        Logger log = Logger.getInstance();
        log.setMinLevel(Logger.Level.WARN);
        File configFile = null;

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
                    configFile = new File(currOpt.getSecond());
                    break;
                case 'h':
                    usage();
                    System.exit(0);
                    break;
                default:
            }
        }

        if (configFile == null) {
            log.error("No config provided");
            System.exit(1);
        }

        //start router
        RouterConfig routerConfig = new RouterConfig(configFile);
        log.error("Starting on ports " + routerConfig.getRSTport() + " " + routerConfig.getCSTPort());
        Thread router = new Thread(new Router(routerConfig));
        router.start();
    }

    /**
     * Prints the usage of the server
     */
    public static void usage() {
        System.out.println("Usage: ");
        System.out.println("  peer");
        System.out.println("  peer --help");
        System.out.println("  peer --config <config_file>");
        System.out.println("Options: ");
        System.out.printf("  %-15s %-20s\n", "-h, --help", "Show this help message");
        System.out.printf("  %-15s %-20s\n", "-c, --config", "Sets the config file for the peer");
    }
}
