package common.I2P.router;

import common.I2P.I2NP.*;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.Lease;
import common.I2P.NetworkDB.LeaseSet;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.tunnels.Tunnel;
import common.I2P.tunnels.TunnelManager;
import common.Logger;
import common.transport.I2NPSocket;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class represents Core Router module of I2P
 */
public class Router implements Runnable {
    /**
     * TunnelManager is responsible for managing tunnels
     */
    public TunnelManager tunnelManager;

    /**
     * Socket is for connecting to client using I2CP protocol
     */
    ServerSocket clientSock;

    /**
     * NetworkDB is the database of all routers in the network
     */
    NetDB netDB;;

    /**
     * RouterID is the ID of this router
     */
    public RouterID routerID;
    // CHANGE THIS TO PRIVATE LATER!!!! I JUST NEEDED IT FOR A TEST

    /**
     * Port of the router
     */
    int port;

    /**
     * RouterInfo is the information about this router
     */
    public RouterInfo routerInfo;

    /**
     * ElGamal key pair for this router
     */
    KeyPair elgamalKeyPair;

    /**
     * DSA-SHA1 key pair for this router
     */
    KeyPair edKeyPair;

    /**
     * Create router from specified config file
     * 
     * @param configFile JSON File to use for configuration
     */
    Router(File configFile) throws IOException {
        // todo add config parsing
        int port = 7000; // hard coded for now we will fix later
        this.port = port;
        int boot = 8080;
        setUp(port, boot, false); // change later
    }

    /**
     * Create router using a default config file
     */
    public Router(int port, int bootstrapPort, boolean isFloodfill) throws IOException {
        // todo add config parsing
        this.port = port;
        this.tunnelManager = new TunnelManager();
        setUp(port, bootstrapPort, isFloodfill);
    }

    private void setUp(int port, int bootstrapPort, boolean isFloodfill) throws IOException {
        // speciality floodfill router
        Security.addProvider(new BouncyCastleProvider()); // Add BouncyCastle provider for cryptography

        // Bind the socket to the router's port
        I2NPSocket socket = new I2NPSocket(port, InetAddress.getByName("127.0.0.1"));

        // Generate keys and create RouterInfo
        elgamalKeyPair = generateKeyPairElGamal();
        edKeyPair = generateKeyPairEd();
        routerID = new RouterID(elgamalKeyPair.getPublic(), edKeyPair.getPublic());
        routerInfo = new RouterInfo(routerID, System.currentTimeMillis(), "127.0.0.1", port, edKeyPair.getPrivate());

        // Initialize NetDB
        netDB = new NetDB(routerInfo);

        // Send a DatabaseStore message to the bootstrap peer
        DatabaseStore databaseStore = new DatabaseStore(routerInfo); // reply token set to 0 for now yay!
        Random random = new Random();
        int msgId = random.nextInt(); // random message id for now
        I2NPHeader msg = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, msgId, System.currentTimeMillis() + 1000,
                databaseStore);
        socket.sendMessage(msg, "127.0.0.1", bootstrapPort);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // wait for the message to be sent

        // send of self from self to bootstrap - get bootstrap info (if same get
        // boostrap info)
        DatabaseLookup databaseLookup = new DatabaseLookup(routerInfo.getHash(), routerInfo.getHash());
        I2NPHeader lookupMsg = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, 1, System.currentTimeMillis() + 1000,
                databaseLookup);
        socket.sendMessage(lookupMsg, "127.0.0.1", bootstrapPort);

        // give enough time for all the routers to send their messages/turn on
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(5000); // 5 seconds to wait for the message to be sent while we turn them all on
                    DatabaseLookup databaseLookup2 = new DatabaseLookup(new byte[32], routerInfo.getHash());
                    I2NPHeader lookupMsg2 = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, random.nextInt(),
                            System.currentTimeMillis() + 100,
                            databaseLookup2);
                    socket.sendMessage(lookupMsg2, "127.0.0.1", bootstrapPort);
                    // netDB.getKClosestRouterInfos(routerID.getHash(), 10);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        t1.start(); // will go to routerservicethread after (pray)

        // wait for client request
        Thread t2 = new Thread(new Runnable() {
            public void run() {
                try {
                    Logger.getInstance().debug("Net db " + netDB.logNetDB());
                    Thread.sleep(10000);
                    // create tunnel build for 3 hops
                    Random random = new Random();
                    int tunnelID = random.nextInt(1000); // random tunnel id for now
                    createTunnelBuild(3, tunnelID, true); // make inbound
                    Thread.sleep(1000); // wait for the message to be sent
                    createTunnelBuild(3, tunnelID, false); // make outbound
                    // double check this later
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });
        t2.start();

        // Start the router service thread to handle incoming messages
        ExecutorService threadpool = Executors.newFixedThreadPool(5);
        while (true) {
            I2NPHeader message = socket.getMessage();
            RouterServiceThread rst = new RouterServiceThread(netDB, routerInfo, message, tunnelManager, edKeyPair.getPrivate());
            // To sam, this will turn on floodfill, from your favorite NetDB implementor
            // Seth
            // rst.setFloodFill(true);
            threadpool.execute(rst);
        }
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
        System.out.println("QueryNetDBForRouters returned " + closestRouters.size() + " routers.");
        return closestRouters;
    }

    private static KeyPair generateKeyPairElGamal() {
        // Generate a key pair for the router
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ElGamal");
            keyGen.initialize(2048); // 2048 bits for RSA
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ed signing keypair generation
    private static KeyPair generateKeyPairEd() {
        // Generate a key pair for the router
        try {
            KeyPairGenerator keyGen = null;
            try {
                keyGen = KeyPairGenerator.getInstance("Ed25519", "BC");
            } catch (NoSuchProviderException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    // this is for building the tunnels
    public void createTunnelBuild(int numHops, int tunnelID, boolean isInbound) throws NoSuchAlgorithmException {
        Random random = new Random();
        ArrayList<TunnelBuild.Record> records = new ArrayList<>();

        // actually get list of peers from netdb
        ArrayList<RouterInfo> tempPeers = queryNetDBForRouters(numHops);
        if (tempPeers == null || tempPeers.isEmpty()) {
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
            tunnelManager.addInboundTunnel(tunnelID, potentialTunnel);
        } else {
            tunnelManager.addOutboundTunnel(tunnelID, potentialTunnel);
        }

        int sendMsgID = random.nextInt();
        long requestTime = System.currentTimeMillis() / 1000;

        ArrayList<TunnelHopInfo> hopInfo = new ArrayList<>(); // this is for the hops in the tunnel

        for (int i = tempPeers.size() - 1; i >= 0; i--) {
            RouterInfo current = tempPeers.get(i);

            byte[] toPeer = Arrays.copyOf(current.getRouterID().getHash(), 16); // only first 16 bytes of the hash
            int receiveTunnel = tunnelID; // tunnel id for the tunnel
            byte[] ourIdent = routerID.getHash(); // its okay for each hop to see this cause they have the tunnel id



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
            if (i == 0) {
                position = TunnelBuild.Record.TYPE.GATEWAY;
                hopInfoInput = new ArrayList<>(hopInfo);
                next = tempPeers.get(i+1);
                // set the hop info for the first hop
            } else if (i == tempPeers.size() - 1) {
                position = TunnelBuild.Record.TYPE.ENDPOINT;
                if (isInbound) {
                    next = routerInfo; // set to client creating request if real for testing set to gateway router
                    // PELASE TREMEMBER TO CHANG ETHIS SAM OMG PLEAS JEHGEAH FG SGF
                } else {
                    System.out.println("attmpting...");
                    //below code will not work we store our routerInfo under our routerID
                    common.I2P.NetworkDB.Record record = netDB.lookup(routerID.getHash()); // get the lease set for the destination which is ourself
                    System.out.println("Record is: " + record.toString());
                    LeaseSet leaseSet = (LeaseSet) record; // get the lease set for the destination
                    // apparently it thinks this is a router info and not a lease set so uhhh well come back to this
                    // this is the destination temp client would do this during creation instead

                    HashSet<Lease> leases = leaseSet.getLeases(); // get the router info for the destination
                    // just select the first lease for now cause theres only one
                    Lease lease = leases.iterator().next();
                    next = (RouterInfo) netDB.lookup(lease.getTunnelGW()); // get the router info for the destination
                    System.out.println("Next hop is: " + Arrays.toString(next.getRouterID().getHash()));
                }
            } else {
                position = TunnelBuild.Record.TYPE.PARTICIPANT;
                next = tempPeers.get(i+1);
            }
            int nextTunnel = tunnelID;
            byte[] nextIdent = next.getHash();

            TunnelBuild.Record record = new TunnelBuild.Record(
                    toPeer,
                    receiveTunnel,
                    ourIdent,
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
            System.out.println("Added record for peer: " + Arrays.toString(toPeer));

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
        try {
            I2NPSocket buildSocket = new I2NPSocket();
            buildSocket.sendMessage(tunnelBuildMessage, firstPeer);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private SecretKey generateAESKey(int bits) {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IllegalStateException("AES KeyGenerator instance could not be created.", e);
        }
        keyGen.init(bits);
        return keyGen.generateKey();
    }

    @Override
    public void run() {
    }
}
