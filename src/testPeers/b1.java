package testPeers;

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

public class b1 {
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
            RouterInfo routerInfo = new RouterInfo(routerID, System.currentTimeMillis(), "127.0.0.1", 6969, signingprivKey);

            NetDB netDB = new NetDB(routerInfo);
            I2NPSocket sock = new I2NPSocket( 8080, InetAddress.getByName("127.0.0.1"));
            ExecutorService threadpool = Executors.newFixedThreadPool(10);

            while(true) {
                I2NPHeader message = sock.getMessage();
                threadpool.execute(new RouterServiceThread(netDB, routerInfo, message));

            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
