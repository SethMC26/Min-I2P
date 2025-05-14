package common.I2P.router;

import common.I2P.I2NP.DatabaseLookup;
import common.I2P.I2NP.DatabaseStore;
import common.I2P.I2NP.I2NPHeader;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.tunnels.TunnelManager;
import common.Logger;
import common.transport.I2CP.I2CPMessage;
import common.transport.I2NPSocket;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class represents Core Router module of I2P
 */
public class Router implements Runnable {
    /**
     * Port for Client Service Thread
     */
    private int CSTPort;
    /**
     * Address for bootstrap peer
     */
    private InetSocketAddress bootstrapAddress;
    private int RSTPort;
    /**
     * Address of this router
     */
    private InetAddress address;
    /**
     * TunnelManager is responsible for managing tunnels
     */
    private TunnelManager tunnelManager;
    /**
     * NetworkDB is the database of all routers in the network
     */
    private NetDB netDB;

    /**
     * RouterID is the ID of this router
     */
    private RouterID routerID;

    /**
     * RouterInfo is the information about this router
     */
    private RouterInfo routerInfo;

    /**
     * ElGamal key pair for this router
     */
    private KeyPair elgamalKeyPair;

    /**
     * Ed25519 key pair for this router
     */
    private KeyPair edKeyPair;
    /**
     * Secure random
     */
    private SecureRandom random = new SecureRandom();
    /**
     * Logger for use in Router
     */
    private Logger log = Logger.getInstance();

    /**
     * Create router from specified config file
     * 
     * @param configFile JSON File to use for configuration
     */
    public Router(RouterConfig configFile) {
        this.address = configFile.getAddress();
        this.RSTPort = configFile.getRSTport();
        this.CSTPort = configFile.getCSTPort();
        this.bootstrapAddress = configFile.getBootstrapPeer();
        this.tunnelManager = new TunnelManager();
    }

    /**
     * Create new router which can will be fully setup in a new thread
     * @param address Address of machine router is running on
     * @param RSTport Port to Router Service thread for I2NP communication(Router<->Router)
     * @param CSTPort Port to run Client Service thread for I2CP communication(Client<->Router)
     * @param bootstrapPeer Address of bootstrap peer
     */
    public Router(InetAddress address, int RSTport, int CSTPort, InetSocketAddress bootstrapPeer) {
        // todo add config parsing
        this.address = address;
        this.RSTPort = RSTport;
        this.CSTPort = CSTPort;
        this.bootstrapAddress = bootstrapPeer;
        this.tunnelManager = new TunnelManager();
    }

    private boolean setUp() throws IOException {
        // create socket to contact bootstrap peer
        I2NPSocket socket = new I2NPSocket();

        // Send a DatabaseStore message to the bootstrap peer
        DatabaseStore databaseStore = new DatabaseStore(routerInfo); // reply token set to 0 for now yay!
        int msgId = random.nextInt(); // random message id for now
        I2NPHeader msg = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, msgId, System.currentTimeMillis() + 500,
                databaseStore);
        socket.sendMessage(msg, bootstrapAddress);

        try {
            Thread.sleep(1000); //wait to see if peer got message
            // send of self from self to bootstrap - get bootstrap info (if same get
            // boostrap info)
            DatabaseLookup databaseLookup = new DatabaseLookup(routerInfo.getHash(), routerInfo.getHash());
            I2NPHeader lookupMsg = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, random.nextInt(),
                    System.currentTimeMillis() + 500,
                    databaseLookup);
            socket.sendMessage(lookupMsg, bootstrapAddress);
            // give enough time for all the routers to send their messages/turn on
            Thread.sleep(1000);
            //check if we learned about bootstrap peer netDB should have at least 1 peer(bootstrap)
            if (netDB.getKClosestPeers(routerInfo.getHash(),1).isEmpty()) {
                return false;
            }
        } catch (InterruptedException e) {
            log.warn("Sleeping interrupted attempting to continue setup", e);
        } // wait for the message to be sent

        //send null key to discover peers
        DatabaseLookup databaseLookup2 = new DatabaseLookup(new byte[32], routerInfo.getHash());
        I2NPHeader lookupMsg2 = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, random.nextInt(),
                System.currentTimeMillis() + 100,
                databaseLookup2);
        socket.sendMessage(lookupMsg2, bootstrapAddress);
        return true;
    }

    private KeyPair generateKeyPairElGamal() {
        // Generate a key pair for the router
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ElGamal");
            keyGen.initialize(2048); // 2048 bits for RSA
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // ed signing keypair generation
    private KeyPair generateKeyPairEd() {
        // Generate a key pair for the router
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("Ed25519", "BC");
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e); // should never hit case
        }
    }
    /**
     * Start router
     */
    @Override
    public void run() {
        // Generate keys and create RouterInfo
        elgamalKeyPair = generateKeyPairElGamal();

        edKeyPair = generateKeyPairEd();
        routerID = new RouterID(elgamalKeyPair.getPublic(), edKeyPair.getPublic());
        routerInfo = new RouterInfo(routerID, System.currentTimeMillis(), address.getHostName(), RSTPort, edKeyPair.getPrivate());

        // Initialize NetDB
        netDB = new NetDB(routerInfo);

        //hashmap for RST to CST communication
        ConcurrentHashMap<Integer, ConcurrentLinkedQueue<I2CPMessage>> clientMessages = new ConcurrentHashMap<>();

        try {
            //create and start RST
            Thread rst = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Start the router service thread to handle incoming messages
                    try {
                        I2NPSocket socket = new I2NPSocket(RSTPort, address);
                        ExecutorService threadpool = Executors.newFixedThreadPool(15);
                        while (true) {
                            I2NPHeader message = null;

                            message = socket.getMessage();
                            RouterServiceThread rst = new RouterServiceThread(netDB, routerInfo, message, clientMessages,
                                    tunnelManager, elgamalKeyPair.getPrivate(), edKeyPair.getPrivate());
                            // rst.setFloodFill(true);
                            threadpool.execute(rst);
                        }
                    }
                    catch(SocketException e) {
                        log.error("Fatal could not setup socket for RST ", e);
                        throw new RuntimeException(e);
                    }
                    catch(IOException e) {
                        log.warn("RST: IO error while getting message", e);
                    }
                }
            });
            rst.start(); //start router service thread

            if (bootstrapAddress.getPort() == RSTPort) { //we are bootstrap peer no clients and no setup needed
                return;
            }

            //create and start CST
            Thread cst = new Thread(new ClientServiceThread(routerInfo, tunnelManager, netDB, CSTPort, clientMessages));
            cst.start();

            //attempt bootstrap process 5 times if we still cannot connect to network we will fail
            //each attempt takes about ~2 seconds
            boolean startup = false;
            for (int i = 0; i < 5; i++) {
                if (setUp()) {
                    log.info("Bootstrap successful");
                    startup = true;
                    break;
                }
            }

            if (!startup) {
                log.error("Bootstrap failed even after 5 attempts");
                log.info("Please try restarting router make sure Bootstrap peer online");
                System.exit(1);
            }

        } catch (IOException e) {
            log.error("Fatal could not setup router", e);
            throw new RuntimeException(e);
        }
    }
}
