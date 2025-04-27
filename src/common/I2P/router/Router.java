package common.I2P.router;

import common.I2P.I2NP.DatabaseStore;
import common.I2P.I2NP.I2NPHeader;
import common.I2P.I2NP.I2NPMessage;
import common.I2P.I2NP.TunnelBuild;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.tunnels.TunnelEndpoint;
import common.I2P.tunnels.TunnelManager;
import common.I2P.tunnels.TunnelParticipant;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

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
     * RouterID is the ID of this router
     */
    RouterID routerID;

    /**
     * ElGamal key pair for this router
     */
    KeyPair elgamalKeyPair;

    /**
     * DSA-SHA1 key pair for this router
     */
    KeyPair dsaKeyPair;

    /**
     * Create router from specified config file
     * 
     * @param configFile JSON File to use for configuration
     */
    Router(File configFile) throws IOException {
        // todo add config parsing
        setUp();
    }

    /**
     * Create router using a default config file
     */
    Router() throws IOException {
        // todo add config parsing
        setUp();
    }

    private void setUp() throws IOException {
        // todo set proper date from config
        // todo add bootstrap peer
        // setup server socket to communicate with client(application layer)
        clientSock = new ServerSocket(7000); // hard coded for now we will fix later

        // todo connect to bootstrap peer to get initial networkdb and tunnels

        // create a router ID for this router and add self to network database
        // generate elgamal keypair for this router
        elgamalKeyPair = generateKeyPairElGamal(); // generate a random keypair
        PublicKey elgamalPublicKey = elgamalKeyPair.getPublic(); // get the public key

        // generate DSA-SHA1 keypair for this router
        dsaKeyPair = generateKeyPairDSASHA1(); // generate a random keypair
        PublicKey dsaPublicKey = dsaKeyPair.getPublic(); // get the public key

        routerID = new RouterID(elgamalPublicKey, dsaPublicKey); // generate a random router ID
    }

    /**
     * Create a tunnel build message for the given path and tunnel ID
     */
    public TunnelBuild createTunnelBuild(List<RouterID> path, int tunnelID) {
        List<TunnelBuild.Record> records = new ArrayList<>();

        for (int i = 0; i < path.size(); i++) {
            RouterID currentRouter = path.get(i);
            RouterID nextRouter = (i + 1 < path.size()) ? path.get(i + 1) : null;

            // generate layer keys
            SecretKey layerKey = generateAESKey();
            SecretKey ivKey = generateAESKey();
            SecretKey replyKey = generateAESKey();
            byte[] replyIv = generateIV(16);

            TunnelBuild.Record record = new TunnelBuild.Record(
                    currentRouter.getHash(), // who this record is addressed to
                    tunnelID,
                    routerID.getHash(),
                    tunnelID + 1,
                    (nextRouter != null) ? nextRouter.getHash() : null,
                    layerKey, ivKey, replyKey, replyIv,
                    System.currentTimeMillis(),
                    generateMessageID());

            records.add(record);
        }

        return new TunnelBuild(records);
    }

    private byte[] encryptWithElGamal(byte[] data, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("ElGamal/None/PKCS1Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt with ElGamal", e);
        }
    }

    private byte[] decryptWithElGamal(byte[] data, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("ElGamal/None/PKCS1Padding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt with ElGamal", e);
        }
    }

    private int generateMessageID() {
        // todo generate a random message ID for the message
        // for now we will just return a random number
        return (int) (Math.random() * Integer.MAX_VALUE);
    }

    private SecretKey generateAESKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256); // Use 256-bit AES keys
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("AES algorithm not available", e);
        }
    }

    private byte[] generateIV(int size) {
        byte[] iv = new byte[size];
        new SecureRandom().nextBytes(iv);
        return iv;
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

    private static KeyPair generateKeyPairDSASHA1() {
        // Generate a key pair for the router
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
            keyGen.initialize(1024); // 1024 bits for DSA
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Route incoming I2NP messages to appropriate tunnel
     * 
     * @param tunnelID Integer ID of tunnel to route message to
     * @param message  I2NPMessage to route
     */
    public void routeMessage(Integer tunnelID, I2NPMessage message) throws IOException {
        // todo check if tunnel is inbound or outbound and route accordingly

    }

    public void handleBuildRequest(TunnelBuild buildRequest) {
        
    }

    @Override
    public void run() {
        // todo here we will manage tunnels and also client messages

        // example I2NP message for Sam :3
        DatabaseStore storemsg = new DatabaseStore((RouterInfo) null);
        // header effectively will wrap an I2NPMessage
        I2NPHeader I2NPmsg = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, 1111, System.currentTimeMillis(), storemsg);
    }

    public void handleMessage(byte[] encryptedMessage) {
        // Decrypt the outermost layer of the message
        byte[] decryptedMessage = decryptWithElGamal(encryptedMessage, elgamalKeyPair.getPrivate());

    }
}
