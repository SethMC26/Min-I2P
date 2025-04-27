package common.I2P.router;

import common.I2P.I2NP.DatabaseStore;
import common.I2P.I2NP.I2NPHeader;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.tunnels.TunnelManager;
import common.transport.I2NPSocket;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
/**
 * Class represents Core Router module of I2P
 */
public class Router implements Runnable {
    /**
     * TunnelManager is responsible for managing tunnels
     */
    TunnelManager tunnelManager = new TunnelManager();

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
    RouterInfo routerInfo;

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
        I2NPHeader msg = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, 1, System.currentTimeMillis() + 1000, databaseStore);
    
        socket.sendMessage(msg, "127.0.0.1", bootstrapPort);

        // Start the router service thread to handle incoming messages
        ExecutorService threadpool = Executors.newFixedThreadPool(5);
        while (true) {
            I2NPHeader message = socket.getMessage();
            threadpool.execute(new RouterServiceThread(netDB, routerInfo, message));
        }
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

    @Override
    public void run() {
    }
}
