package common.I2P.router;

import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.tunnels.TunnelManager;

import java.io.File;
import java.io.IOException;

import java.net.ServerSocket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

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
    KeyPair dsaKeyPair;

    /**
     * Create router from specified config file
     * 
     * @param configFile JSON File to use for configuration
     */
    Router(File configFile) throws IOException {
        // todo add config parsing
        int port = 7000; // hard coded for now we will fix later
        setUp(port);
    }

    /**
     * Create router using a default config file
     */
    public Router(int port, int bootstrapPort) throws IOException {
        // todo add config parsing
        this.port = port;
        setUp(port);
    }

    private void setUp(int port) throws IOException {
        // todo set proper date from config
        // todo add bootstrap peer
        // setup server socket to communicate with client(application layer)
        clientSock = new ServerSocket(port); // hard coded for now we will fix later

        // create a router ID for this router and add self to network database
        // generate elgamal keypair for this router
        elgamalKeyPair = generateKeyPairElGamal(); // generate a random keypair
        PublicKey elgamalPublicKey = elgamalKeyPair.getPublic(); // get the public key

        // generate DSA-SHA1 keypair for this router
        dsaKeyPair = generateKeyPairDSASHA1(); // generate a random keypair
        PublicKey dsaPublicKey = dsaKeyPair.getPublic(); // get the public key

        routerID = new RouterID(elgamalPublicKey, dsaPublicKey); // generate a random router ID

        // create router info for this router
        routerInfo = new RouterInfo(routerID, (Long) null, "127.0.0.1", port, null);

        // create network database for this router
        netDB = new NetDB(routerInfo); // create a new network database for this router
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

    @Override
    public void run() {
    }
}
