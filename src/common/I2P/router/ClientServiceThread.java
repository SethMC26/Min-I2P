package common.I2P.router;

import common.I2P.I2NP.DatabaseLookup;
import common.I2P.I2NP.I2NPHeader;
import common.I2P.I2NP.TunnelBuild;
import common.I2P.I2NP.TunnelHopInfo;
import common.I2P.IDs.Destination;
import common.I2P.NetworkDB.Record;
import common.I2P.tunnels.Tunnel;
import common.I2P.tunnels.TunnelManager;
import common.I2P.NetworkDB.*;
import common.Logger;
import common.transport.I2CP.*;
import common.transport.I2NPSocket;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.ServerSocket;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static common.transport.I2CP.I2CPMessageTypes.*;

public class ClientServiceThread implements Runnable {
    /**
     * Socket for router communications
     */
    private I2NPSocket routerSock;
    /**
     * Router this thread is a apart of
     */
    private RouterInfo router;
    /**
     * Tunnel manager for this router
     */
    private TunnelManager tunnelManager;
    /**
     * Server for client connections
     */
    private ServerSocket server;
    /**
     * Logger
     */
    private Logger log = Logger.getInstance();
    /**
     * Secure random for generating message ids
     */
    private SecureRandom random = new SecureRandom();
    /**
     * Hashmap with client connections stored under session IDs and queues of
     * messages for those sessions
     */
    private HashMap<Integer, ConcurrentLinkedQueue<I2CPMessage>> clientMessages;
    /**
     * Network database
     */
    private NetDB netDB;

    /**
     * Create new thread to service incoming client connects
     * 
     * @param router         This router we are apart of
     * @param netDB          network database for this router
     * @param port           Port for clients to connect on
     * @param clientMessages Queue of messages from client
     * @throws IOException if could not create ServerSocket or I2NPSocket
     */
    public ClientServiceThread(RouterInfo router, TunnelManager tunnelManager, NetDB netDB, int port,
            HashMap<Integer, ConcurrentLinkedQueue<I2CPMessage>> clientMessages) throws IOException {
        this.router = router;
        this.tunnelManager = tunnelManager;
        this.netDB = netDB;
        this.server = new ServerSocket(port);
        this.routerSock = new I2NPSocket();
        this.clientMessages = clientMessages;
    }

    @Override
    public void run() {
        try {
            ExecutorService threadpool = Executors.newFixedThreadPool(1); // we only have one client application so we
                                                                          // dont need multiple threads
            // todo ask sean if it is helpful for the server to have multiple connections
            // wait for connections
            while (true) {
                I2CPSocket clientSock = new I2CPSocket(server.accept());
                threadpool.execute(new ClientConnectionHandler(clientSock));
            }

        } catch (IOException e) {
            log.error("CST: IO exception", e);
        }
    }

    private boolean isTypeBad(I2CPMessage toCheck, I2CPMessageTypes expectedType) {
        if (toCheck.getType() != expectedType) {
            log.warn("Bad type of message, got " + toCheck.getType() + " but expected " + expectedType);
            log.debug("Message with bad type " + toCheck.toJSONType().getFormattedJSON());
            return true;
        }
        return false;
    }

    private class ClientConnectionHandler implements Runnable {
        /**
         * Socket for client communication for this session
         */
        private I2CPSocket clientSock; // socket to communicate with client
        /**
         * ID of session
         */
        private int sessionID;
        /**
         * Queue of destinations from the client received by the router
         */
        private ConcurrentLinkedQueue<I2CPMessage> messagesFromDest;
        private int lastInboundTunnelID;
        private RouterInfo lastInboundFirstPeer;

        /**
         * Create new connection for the client
         * 
         * @param clientSock I2CP socket for communication with the client
         */
        ClientConnectionHandler(I2CPSocket clientSock) {
            this.clientSock = clientSock;
        }

        @Override
        public void run() {
            try {
                log.debug("CST: Got connection from client");
                I2CPMessage recvMsg = clientSock.getMessage();

                if (recvMsg.getType() == DESTLOOKUP) { // Simple session preform lookup, reply then return
                    DestinationLookup lookup = (DestinationLookup) recvMsg;
                    Destination dest = destLookup(lookup.getHash());
                    clientSock.sendMessage(new DestinationReply(0, dest));
                    return;
                }

                if (isTypeBad(recvMsg, CREATESESSION)) { // bad type we(router) will refuse connection
                    clientSock.sendMessage(new SessionStatus(recvMsg.getSessionID(), SessionStatus.Status.REFUSED));
                    clientSock.close();
                    
                    return;
                }

                // here we will handle creating inbound tunnels for destination
                CreateSession createSession = (CreateSession) recvMsg;
                // generate new id for session
                sessionID = random.nextInt();
                // create new queue of message for sessionID(messages for this client)
                messagesFromDest = new ConcurrentLinkedQueue<>();
                clientMessages.put(sessionID, messagesFromDest);

                // todo sam plz help me create inbound tunnels for this destination
                // we can store these inbound tunnels under the sessionID to identifiy them to
                // this client
                Destination clientDestination = createSession.getDestination();

                // This will generate the inbound and outbound tunnels for the client
                buildTunnel(clientDestination, router, true); // inbound
                try {
                    Thread.sleep(1000); // wait for tunnel to be created for a second
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    // this likely wont happen but if it does we will just ignore it
                    log.warn("CST-CCH: Tunnel creation wait interrupted", e);
                }
                buildTunnel(clientDestination, router, false); // outbound

                // accept session
                clientSock.sendMessage(new SessionStatus(sessionID, SessionStatus.Status.CREATED));

                // todo setup necessary information in router, hashmap with
                // sessionIDs/destinations/messageQueues?
                // give client leases to authorize
                ArrayList<Lease> leases = new ArrayList<>();
                leases.add(new Lease(router.getRouterID(), 0)); // todo sam plz replace the tunnelID with proper one
                                                                // thanks you <3

                RequestLeaseSet requestLeaseSet = new RequestLeaseSet(sessionID, leases);

                // ask client to authorize leaseSets
                clientSock.sendMessage(requestLeaseSet);
                // get authorization
                recvMsg = clientSock.getMessage();
                isTypeBad(recvMsg, CREATELEASESET);

                CreateLeaseSet createLeaseSet = (CreateLeaseSet) recvMsg;
                LeaseSet leaseSet = createLeaseSet.getLeaseSet();
                // this is the private key for elgamal stuff corresponding to public key in
                // leaseset for encryption
                PrivateKey privateKey = createLeaseSet.getPrivateKey();
                // todo sam plz use da leaseset and private keys yuh

                while (true) { // might be a better way to do this that avoids busy waiting
                    // wait until a new message on socket or a new message has arrived from router
                    if (!clientSock.hasMessage() && messagesFromDest.isEmpty())
                        continue;

                    // deal with client messages
                    if (clientSock.hasMessage()) {
                        recvMsg = clientSock.getMessage();
                        log.debug("Handling message type " + recvMsg.getType());
                        switch (recvMsg.getType()) {
                            case SENDMESSAGE -> {
                                SendMessage send = (SendMessage) recvMsg;

                                clientSock.sendMessage(send); // for testing just echo message back
                                // todo set up session information in router
                                // routerSock.send(router, new I2NPHeader()) //send tunnel data to router to
                                // send on outbound tunnel
                            }
                            case DESTLOOKUP -> {
                                DestinationLookup lookup = (DestinationLookup) recvMsg;
                                Destination dest = destLookup(lookup.getHash());
                                clientSock.sendMessage(new DestinationReply(sessionID, dest));
                            }
                            case DESTROYSESSION -> {
                                clientSock.close();
                                clientMessages.remove(sessionID);
                                // todo remove inbound tunnels?
                                // todo handle any necessary session destroying in router
                                return; // close thread
                            }
                            default -> {
                                log.error("Bad type received from client only send and destroy allowed "
                                        + recvMsg.getType());
                                MessageStatus status = new MessageStatus(sessionID, 0, new byte[4],
                                        MessageStatus.Status.BADMESSAGE);
                                clientSock.sendMessage(status);
                            }
                        }
                    }

                    if (!messagesFromDest.isEmpty()) {
                        I2CPMessage message = messagesFromDest.remove();
                        // todo handle getting message from router and giving it back to the client
                        if (message.getType() != SENDMESSAGE) {
                            log.warn("Bad message from router");
                            continue;
                        }

                        clientSock.sendMessage(message);
                    }
                }
            } catch (InvalidObjectException e) {
                log.error("CST-CCH: Bad message ", e);
                try {
                    clientSock.sendMessage(new SessionStatus(sessionID, SessionStatus.Status.DESTROYED));
                    clientSock.close();
                } catch (IOException ex) {
                    log.warn("Could not close client sock", e);
                }
            } catch (IOException e) {
                log.error("CST-CCH: IOException occured", e);
                try {
                    clientSock.sendMessage(new SessionStatus(sessionID, SessionStatus.Status.DESTROYED));
                    clientSock.close();
                } catch (IOException ex) {
                    log.warn("Could not close client sock", e);
                }
            }
        }

        /**
         * Create inbound tunnel for the client destination
         * 
         * @param clientDestination Destination of the client
         * @param router            RouterInfo of this router
         */
        private void buildTunnel(Destination clientDestination, RouterInfo router, boolean isInbound) {
            // note: client destination is not hard set to endpoint of tunnel
            // it is tacked on to the messge itself so the endpoint knows where to forward

            // hold the records for the tunnel
            ArrayList<TunnelBuild.Record> records = new ArrayList<>();

            // generate a tunnel id for the tunnel
            int tunnelID = random.nextInt(1, Integer.MAX_VALUE);

            // generate random number of hops from 3 to 5
            int numHops = random.nextInt(3) + 3; // 3 to 5 hops

            // search netDB by hash of routerID for all closest router to form tunnel with
            ArrayList<RouterInfo> tempPeers = netDB.getKClosestRouterInfos(router.getHash(), numHops + 1);

            if (tempPeers == null || tempPeers.isEmpty()) {
                log.warn("Peer list too small - try restarting router could be timing issue");
                throw new IllegalStateException(
                        "tempPeers is null or empty. Ensure queryNetDBForRouters returns valid data.");
            }

            // Because this is an inbound tunnel, we set the last peer in the list to be
            // this router
            if (isInbound) {
                // set last hop to be the router id of this router
                tempPeers.set(tempPeers.size() - 1, router);
            } else {
                // set first hop to be the router id of this router
                tempPeers.set(0, router);
            }

            // save this list of peers to the tunnel manager for easy access later
            Tunnel potentialTunnel = new Tunnel(tempPeers);
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

            int sendMessageID = random.nextInt(1, Integer.MAX_VALUE); // unique message id for this message
            long requestTime = System.currentTimeMillis(); // time of request

            // Generate unique tunnel IDs per hop
            int[] hopTunnelIDs = new int[tempPeers.size()];

            for (int i = 0; i < tempPeers.size(); i++) {
                if (i == 0) {
                    hopTunnelIDs[i] = tunnelID; // first hop is the tunnel id
                } else {
                    hopTunnelIDs[i] = random.nextInt(1, Integer.MAX_VALUE); // random id for the rest of the hops
                }
            }

            ArrayList<TunnelHopInfo> hopInfo = new ArrayList<>(); // this is for the hops in the tunnel

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
                } else if (i == tempPeers.size() - 1) {
                    position = TunnelBuild.Record.TYPE.ENDPOINT;
                    if (isInbound) {
                        next = router; // set to client creating request if real for testing set to gateway router
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
                        sendMessageID,
                        position,
                        hopInfoInput,
                        replyFlag); // pass the hop info to the record

                records.add(record);
            }

            // send tunnel build message to the first peer in the list
            RouterInfo firstPeer = tempPeers.get(0);
            I2NPHeader tunnelBuildMessage = new I2NPHeader(I2NPHeader.TYPE.TUNNELBUILD, random.nextInt(),
                    System.currentTimeMillis() + 100, new TunnelBuild(records));
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

        /**
         * Generate a new AES key for the tunnel
         * 
         * @param i Size of the key in bits
         * @return SecretKey for the tunnel
         */
        private SecretKey generateAESKey(int i) {
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(i);
                return keyGen.generateKey();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e); // should never hit case
            }
        }

        /**
         * Look up destination, if we do not have it we will
         * 
         * @param hash Hash of destination to lookup
         * @return Destination if found null otherwise
         * @throws IOException
         */
        private Destination destLookup(byte[] hash) throws IOException {
            Record record = netDB.lookup(hash);
            if (record == null || record.getRecordType() == Record.RecordType.ROUTERINFO) { // could not find or is
                                                                                            // wrong type

                // see if our peers have message
                ArrayList<RouterInfo> peers = netDB.getKClosestRouterInfos(hash, 3);

                for (RouterInfo peer : peers) {
                    I2NPHeader lookup = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, random.nextInt(),
                            System.currentTimeMillis() + 10, new DatabaseLookup(hash, router.getHash()));
                    routerSock.sendMessage(lookup, peer);
                }
                // wait for responses
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    log.warn("Dest lookup wait interrupted", e);
                }
                // attempt lookup again
                record = netDB.lookup(hash);
                if (record == null || record.getRecordType() == Record.RecordType.ROUTERINFO)
                    return null; // bad type of record should be leaseset for destination
            }
            LeaseSet leaseSet = (LeaseSet) record;
            return leaseSet.getDestination();
        }
    }
}
