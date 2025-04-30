package testPeers;

import common.I2P.I2NP.DatabaseLookup;
import common.I2P.I2NP.DatabaseStore;
import common.I2P.I2NP.I2NPHeader;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.router.RouterServiceThread;
import common.Logger;
import common.transport.I2NPSocket;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.net.InetAddress;
import java.security.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class p2 {
    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        Logger log = Logger.getInstance();
        log.setMinLevel(Logger.Level.DEBUG);


        try {
            // Generate a pair of keys. Using Elgamal key generation.
            // The size of the key should be 512-bits. Anything smaller is
            // too small for practical purposes.
            KeyPairGenerator elgamalKeyGen = KeyPairGenerator.getInstance(
                    "ElGamal");
            elgamalKeyGen.initialize(512);
            KeyPair elgamalPair = elgamalKeyGen.generateKeyPair();

            // Get the public and private key pair from the generated
            // pair.
            PublicKey pubKey = elgamalPair.getPublic();
            PrivateKey privKey = elgamalPair.getPrivate();


            KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519", "BC");
            KeyPair pair = generator.generateKeyPair();

            // Get the public and private key pair from the generated pair.
            PublicKey signingpubKey = pair.getPublic();
            PrivateKey signingprivKey = pair.getPrivate();

            //create information about this router
            RouterID routerID = new RouterID(pubKey, signingpubKey);
            RouterInfo routerInfo = new RouterInfo(routerID, System.currentTimeMillis(), "127.0.0.1", 7000, signingprivKey);

            NetDB netDB = new NetDB(routerInfo);
            I2NPSocket sock = new I2NPSocket();

            DatabaseStore databaseStore = new DatabaseStore(routerInfo);
            I2NPHeader msg = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, 1, System.currentTimeMillis() + 1000, databaseStore);

            sock.sendMessage(msg, "127.0.0.1", 6969);

            try {
                Thread.sleep(1_000);                 // wait 1000 ms
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();  // restore interrupt status
            }
            //create information about this router
            //System.out.println("enter hash:");
            //String hash = new Scanner(System.in).nextLine();

            //DatabaseLookup databaseLookup = new DatabaseLookup(Base64.decode(hash), routerInfo.getHash());
            DatabaseLookup databaseLookup = new DatabaseLookup(routerInfo.getHash(), routerInfo.getHash());
            //databaseStore.setReply(500, new byte[32]);

            msg = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, 1, System.currentTimeMillis() + 1000, databaseLookup);
            sock.sendMessage(msg, "127.0.0.1", 6969);

            DatabaseLookup databaseLookup2 = new DatabaseLookup(new byte[32], new byte[32]);
            msg = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, 1, System.currentTimeMillis() + 1000, databaseLookup2);
            sock.sendMessage(msg, "127.0.0.1", 8080);

            ExecutorService threadPool = Executors.newFixedThreadPool(5);
            sock = new I2NPSocket(7000, InetAddress.getByName("127.0.0.1"));
            while(true) {
                I2NPHeader recvMessage = sock.getMessage();
                threadPool.execute(new RouterServiceThread(netDB, routerInfo, recvMessage));
            }

        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
