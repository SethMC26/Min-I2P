package common.I2P.router;

import common.I2P.I2NP.DatabaseStore;
import common.I2P.I2NP.I2NPHeader;
import common.I2P.I2NP.TunnelBuild;
import common.I2P.I2NP.TunnelBuild.Record.TYPE;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.tunnels.Tunnel;
import common.I2P.tunnels.TunnelManager;
import common.I2P.NetworkDB.Record;
import common.transport.I2NPSocket;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

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
        setUp(port, boot);
    }

    /**
     * Create router using a default config file
     */
    public Router(int port, int bootstrapPort) throws IOException {
        // todo add config parsing
        this.port = port;
        this.tunnelManager = new TunnelManager();
        setUp(port, bootstrapPort);
    }

    private void setUp(int port, int bootstrapPort) throws IOException {
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
        DatabaseStore databaseStore = new DatabaseStore(routerInfo);
        I2NPHeader msg = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, 1, System.currentTimeMillis() + 1000,
                databaseStore);

        socket.sendMessage(msg, "127.0.0.1", bootstrapPort);

        // Start the router service thread to handle incoming messages
        ExecutorService threadpool = Executors.newFixedThreadPool(5);
        while (true) {
            I2NPHeader message = socket.getMessage();
            threadpool.execute(new RouterServiceThread(netDB, routerInfo, message,
                    tunnelManager));
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
    public TunnelBuild createTunnelBuild(int numHops, int tunnelD, boolean isInbound) throws NoSuchAlgorithmException {
        Random random = new Random();
        List<TunnelBuild.Record> records = new ArrayList<>();

        // actually get list of peers from netdb
        ArrayList<RouterInfo> tempPeers = queryNetDBForRouters(numHops);
        Tunnel potentialTunnel = new Tunnel(tempPeers);

        // add to tunnel manager
        if (isInbound) {
            tunnelManager.addInboundTunnel(tunnelD, potentialTunnel);
        } else {
            tunnelManager.addOutboundTunnel(tunnelD, potentialTunnel);
        }

        int sendMsgID = random.nextInt();
        long requestTime = System.currentTimeMillis() / 1000;

        for (int i = 0; i < tempPeers.size(); i++) {
            RouterInfo current = tempPeers.get(i);
            RouterInfo next = (i + 1 < tempPeers.size()) ? tempPeers.get(i + 1) : null;

            byte[] toPeer = Arrays.copyOf(current.getRouterID().getHash(), 16); // only first 16 bytes of the hash
            int receiveTunnel = tunnelD; // tunnel id for the tunnel
            byte[] ourIdent = routerID.getHash(); // its okay for each hop to see this cause they have the tunnel id

            int nextTunnel = (next != null) ? random.nextInt() : 0;
            byte[] nextIdent = (next != null) ? next.getRouterID().getHash() : new byte[32]; // if no next, blank

            SecretKey layerKey = generateAESKey(256);
            SecretKey ivKey = generateAESKey(256);
            SecretKey replyKey = generateAESKey(256);

            byte[] replyIv = new byte[16];
            random.nextBytes(replyIv);

            TYPE position = null;
            if (i == 0) {
                position = TYPE.GATEWAY;
            } else if (i == tempPeers.size() - 1) {
                position = TYPE.ENDPOINT;
            } else {
                position = TYPE.PARTICIPANT;
            }

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
                    position);

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
        return new TunnelBuild(records);
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
