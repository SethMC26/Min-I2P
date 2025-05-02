package testPeers;

import common.I2P.I2NP.DatabaseLookup;
import common.I2P.I2NP.DatabaseStore;
import common.I2P.I2NP.I2NPHeader;
import common.I2P.IDs.Destination;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.router.ClientServiceThread;
import common.I2P.router.RouterServiceThread;
import common.I2P.tunnels.TunnelManager;
import common.Logger;
import common.transport.I2CP.*;
import common.transport.I2NPSocket;
import merrimackutil.json.types.JSONObject;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.net.InetAddress;
import java.security.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class p1 {
    static int routerPort = 10001;
    static int servicePort = 20001;
    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        Logger log = Logger.getInstance();
        log.setMinLevel(Logger.Level.DEBUG);

        try {
            //run router
            Thread router = new Thread(new Router());
            router.start();

            //this will emulate our client service thread which technically is part of the router
            Thread cst = new Thread(new ClientServiceThread(routerInfo, netDB,servicePort, new ConcurrentLinkedQueue<>()));
            cst.start();

            try {
                Thread.sleep(5_100);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            DatabaseLookup databaseLookup = new DatabaseLookup(new byte[32], routerInfo.getHash());
            //databaseStore.setReply(500, new byte[32]);
            I2NPSocket sock = new I2NPSocket();
            I2NPHeader msg = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, 1, System.currentTimeMillis() + 1000, databaseLookup);
            sock.sendMessage(msg, "127.0.0.1", 8080);

            //this will be our client stuff following
            //connect to clientservicethread
            I2CPSocket sock2 = new I2CPSocket("127.0.0.1", servicePort);
            Destination destination = null;
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
                PublicKey pk = kpg.generateKeyPair().getPublic();
                destination = new Destination(pk);
                System.out.println(destination.toJSONType().getFormattedJSON());
                System.out.println("sending message");
                sock2.sendMessage(new CreateSession(destination));
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            SessionStatus sessionStatus = (SessionStatus) sock2.getMessage();

            int connectionID = sessionStatus.getSessionID(); //id for this session
            JSONObject test = new JSONObject();
            test.put("I get messages from the ", "I2P Router it's like your from another galaxy");

            sock2.sendMessage(new SendMessage(connectionID, destination, new byte[4], test));

            SendMessage send = (SendMessage) sock2.getMessage();
            System.out.println("service echoed message back");
            sock2.sendMessage(new DestroySession(connectionID));
            sock2.close();

            try {
                Thread.sleep(10_100);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            System.out.println(netDB.logNetDB());
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    static RouterInfo routerInfo;
    static NetDB netDB;

    private static class Router implements Runnable{
        @Override
        public void run() {
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
                routerInfo = new RouterInfo(routerID, System.currentTimeMillis(), "127.0.0.1", routerPort, signingprivKey);

                netDB = new NetDB(routerInfo);
                I2NPSocket sock = new I2NPSocket();

                DatabaseStore databaseStore = new DatabaseStore(routerInfo);
                I2NPHeader msg = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, 1, System.currentTimeMillis() + 1000, databaseStore);

                sock.sendMessage(msg, "127.0.0.1", 8080);

                try {
                    Thread.sleep(1_000);                 // wait 1000 ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();  // restore interrupt status
                }

                DatabaseLookup databaseLookup = new DatabaseLookup(routerInfo.getHash(), routerInfo.getHash());
                //databaseStore.setReply(500, new byte[32]);
                msg = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, 1, System.currentTimeMillis() + 1000, databaseLookup);
                sock.sendMessage(msg, "127.0.0.1", 8080);

                ExecutorService threadPool = Executors.newFixedThreadPool(5);
                sock = new I2NPSocket(routerPort, InetAddress.getByName("127.0.0.1"));
                while(true) {
                    I2NPHeader recvMessage = sock.getMessage();
                    threadPool.execute(new RouterServiceThread(netDB, routerInfo, recvMessage, new TunnelManager()));
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
