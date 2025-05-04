package common.I2P.router;

import common.I2P.I2NP.*;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.tunnels.Tunnel;
import common.I2P.tunnels.TunnelManager;
import common.Logger;
import common.transport.I2CP.I2CPMessage;
import common.transport.I2NPSocket;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

    private int lastInboundTunnelID;

    private RouterInfo lastInboundFirstPeer;

    /**
     * Create router from specified config file
     * 
     * @param configFile JSON File to use for configuration
     */
    Router(File configFile) throws IOException {
        // todo add config parsing
        //int port = 7000; // hard coded for now we will fix later
        //this.port = port;
        //int boot = 8080;
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

    private void setUp() throws IOException {
        // create socket to contact bootstrap peer
        I2NPSocket socket = new I2NPSocket();

        // Send a DatabaseStore message to the bootstrap peer
        DatabaseStore databaseStore = new DatabaseStore(routerInfo); // reply token set to 0 for now yay!
        int msgId = random.nextInt(); // random message id for now
        I2NPHeader msg = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, msgId, System.currentTimeMillis() + 500,
                databaseStore);
        socket.sendMessage(msg, bootstrapAddress);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.warn("Sleeping interrupted attempting to continue setup", e);
        } // wait for the message to be sent

        // send of self from self to bootstrap - get bootstrap info (if same get
        // boostrap info)
        DatabaseLookup databaseLookup = new DatabaseLookup(routerInfo.getHash(), routerInfo.getHash());
        I2NPHeader lookupMsg = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, random.nextInt(),
                System.currentTimeMillis() + 500,
                databaseLookup);
        socket.sendMessage(lookupMsg, bootstrapAddress);
        // give enough time for all the routers to send their messages/turn on
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000); // 5 seconds to wait for the message to be sent while we turn them all on
                    DatabaseLookup databaseLookup2 = new DatabaseLookup(new byte[32], routerInfo.getHash());
                    I2NPHeader lookupMsg2 = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, random.nextInt(),
                            System.currentTimeMillis() + 100,
                            databaseLookup2);
                    socket.sendMessage(lookupMsg2, bootstrapAddress);
                    // netDB.getKClosestRouterInfos(routerID.getHash(), 10);
                } catch (InterruptedException e) {
                    log.warn("Sleeping interrupted attempting to continue ", e);
                } catch (IOException e) {
                    log.error("Could not setup/send message to learn about peers", e);
                    log.error("FATAL: step needed to establish connection with peers");
                    throw new RuntimeException(e);
                }
            }
        });
        t1.start(); // will go to routerservicethread after (pray)

        // wait for client request
        Thread t2 = new Thread(new Runnable() {
            public void run() {
                try {
                    Logger.getInstance().debug("Net db " + netDB.logNetDB());
                    Thread.sleep(15000);
                    // create tunnel build for 3 hops
                    int tunnelID = random.nextInt(); // random tunnel id for now
                    createTunnelBuild(3, tunnelID, true); // make inbound
                    Thread.sleep(1000); // wait for the message to be sent
                    System.out.println("Creating outbound tunnel build");
                    createTunnelBuild(3, tunnelID, false); // make outbound
                    // double check this later
                } catch (InterruptedException e) {
                    log.warn("Sleeping interrupted attempting to continue ", e);
                }
            }
        });
        t2.start();

    }

    /**
     * Query the NetDB for other routers on the network.
     *
     * @param k The number of closest routers to retrieve.
     * @return A list of RouterInfo objects for the closest routers.
     */
    public ArrayList<RouterInfo> queryNetDBForRouters(int k) {
        // Use the router's own hash as the key to find closest routers
        byte[] routerHash = routerID.getHash();
        ArrayList<RouterInfo> closestRouters = netDB.getKClosestRouterInfos(routerHash, k);
        return closestRouters;
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

    // this is for building the tunnels
    public void createTunnelBuild(int numHops, int tunnelID, boolean isInbound) {
        ArrayList<TunnelBuild.Record> records = new ArrayList<>();

        // actually get list of peers from netdb
        ArrayList<RouterInfo> tempPeers = queryNetDBForRouters(numHops);
        if (tempPeers == null || tempPeers.isEmpty()) {
            log.warn("Peer list too small - try restarting router could be timing issue");
            throw new IllegalStateException(
                    "tempPeers is null or empty. Ensure queryNetDBForRouters returns valid data.");
        }
        if (isInbound) {
            // set last hop to be the router id of this router
            tempPeers.set(tempPeers.size() - 1, routerInfo);
        } else {
            // set first hop to be the router id of this router
            tempPeers.set(0, routerInfo);
        }

        Tunnel potentialTunnel = new Tunnel(tempPeers);

        // add to tunnel manager
        if (isInbound) {
            System.out.println("Adding inbound tunnel: " + tunnelID);
            tunnelManager.addInboundTunnel(tunnelID, potentialTunnel);

            // Save info for use by outbound tunnel
            this.lastInboundTunnelID = tunnelID;
            this.lastInboundFirstPeer = tempPeers.get(0); // inbound gateway
        } else {
            System.out.println("Adding outbound tunnel: " + tunnelID);
            tunnelManager.addOutboundTunnel(tunnelID, potentialTunnel);
        }

        int sendMsgID = random.nextInt();
        long requestTime = System.currentTimeMillis();

        ArrayList<TunnelHopInfo> hopInfo = new ArrayList<>(); // this is for the hops in the tunnel

        // Generate unique tunnel IDs per hop
        int[] hopTunnelIDs = new int[tempPeers.size()];
        for (int i = 0; i < tempPeers.size(); i++) {
            hopTunnelIDs[i] = random.nextInt(); // Replace with a tunnelID allocator if needed
        }

        for (int i = tempPeers.size() - 1; i >= 0; i--) {
            RouterInfo current = tempPeers.get(i);

            byte[] toPeer = Arrays.copyOf(current.getRouterID().getHash(), 16); // only first 16 bytes of the hash
            int receiveTunnel = hopTunnelIDs[i]; // tunnel id for the tunnel

            SecretKey layerKey = generateAESKey(256);
            SecretKey ivKey = generateAESKey(256);
            SecretKey replyKey = generateAESKey(256);

            byte[] replyIv = new byte[16];
            random.nextBytes(replyIv);

            ArrayList<TunnelHopInfo> hopInfoInput = null; // this is for the hops in the tunnel

            boolean replyFlag = false; // this is for the hops in the tunnel - they change this later

            TunnelHopInfo hopInfoItem = new TunnelHopInfo(toPeer, layerKey, ivKey,
                    receiveTunnel);
            hopInfo.add(0, hopInfoItem); // add to the front of the list

            TunnelBuild.Record.TYPE position = null;

            RouterInfo next;
            int nextTunnel = 0; // this is the tunnel id for the next hop default to 0 for outbound creation
            if (i == 0) {
                position = TunnelBuild.Record.TYPE.GATEWAY;
                hopInfoInput = new ArrayList<>(hopInfo);
                next = tempPeers.get(i + 1);
                nextTunnel = hopTunnelIDs[i + 1]; // tunnel id for the next hop
                // set the hop info for the first hop
            } else if (i == tempPeers.size() - 1) {
                position = TunnelBuild.Record.TYPE.ENDPOINT;
                if (isInbound) {
                    next = routerInfo; // set to client creating request if real for testing set to gateway router
                    // PELASE TREMEMBER TO CHANG ETHIS SAM OMG PLEAS JEHGEAH FG SGF
                } else {
                    // Forward reply through inbound tunnel gateway
                    next = this.lastInboundFirstPeer;
                    nextTunnel = this.lastInboundTunnelID;
                }
            } else {
                position = TunnelBuild.Record.TYPE.PARTICIPANT;
                next = tempPeers.get(i + 1);
                nextTunnel = hopTunnelIDs[i + 1]; // tunnel id for the next hop
            }
            byte[] nextIdent = next.getHash();

            TunnelBuild.Record record = new TunnelBuild.Record(
                    toPeer,
                    receiveTunnel,
                    current.getHash(),
                    nextTunnel,
                    nextIdent,
                    layerKey,
                    ivKey,
                    replyKey,
                    replyIv,
                    requestTime,
                    sendMsgID,
                    position,
                    hopInfoInput,
                    replyFlag); // pass the hop info to the record

            records.add(record);

            // temp disable encryption for testing
            // for (common.I2P.I2NP.TunnelBuild.Record recordItem : records) {
            // // encrypt the entire record with the public key of the peer
            // PublicKey peerPublicKey = current.getRouterID().getElgamalPublicKey(); // use
            // full key
            // // iterate over each record and encrypt it with the public key
            // TunnelBuild.Record encryptedData = recordItem.encrypt(peerPublicKey);
            // recordItem = encryptedData; // oh yeah were overwriting baby
            // }
        }

        // send tunnel build message to the first peer in the list
        RouterInfo firstPeer = tempPeers.get(0);
        I2NPHeader tunnelBuildMessage = new I2NPHeader(I2NPHeader.TYPE.TUNNELBUILD, sendMsgID,
                System.currentTimeMillis() + 1000, new TunnelBuild(records));
        I2NPSocket buildSocket = null;
        try {
            buildSocket = new I2NPSocket();
            buildSocket.sendMessage(tunnelBuildMessage, firstPeer);
            buildSocket.close();
        } catch (IOException e) {
            if (buildSocket != null)
                buildSocket.close(); // if possible close socket if created
            log.error("Fatal: Could not send tunnel build message");
            log.info("Try restarting router");
        }
    }

    private SecretKey generateAESKey(int bits) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(bits);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
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
        HashMap<Integer, ConcurrentLinkedQueue<I2CPMessage>> clientMessages = new HashMap<>();

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
                            RouterServiceThread rst = new RouterServiceThread(netDB, routerInfo, message, tunnelManager,
                                    edKeyPair.getPrivate());
                            // To sam, this will turn on floodfill, from your favorite NetDB implementor
                            // Seth
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

            setUp();

            //create and start CST
            Thread cst = new Thread(new ClientServiceThread(routerInfo, netDB, CSTPort, clientMessages));
            cst.start();

        } catch (IOException e) {
            log.error("Fatal could not setup router", e);
            throw new RuntimeException(e);
        }
    }
}
