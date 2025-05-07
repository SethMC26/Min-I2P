package common.I2P.router;

import common.I2P.I2NP.*;
import common.I2P.IDs.Destination;
import common.I2P.NetworkDB.Record;
import common.I2P.NetworkDB.*;
import common.I2P.tunnels.Tunnel;
import common.I2P.tunnels.TunnelManager;
import common.Logger;
import common.transport.I2CP.*;
import common.transport.I2NPSocket;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.*;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private ConcurrentHashMap<Integer, ConcurrentLinkedQueue<I2CPMessage>> clientMessages;
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
            ConcurrentHashMap<Integer, ConcurrentLinkedQueue<I2CPMessage>> clientMessages) throws IOException {
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

        private ConcurrentLinkedQueue<I2CPMessage> msgQueue;

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
                // accept session
                clientSock.sendMessage(new SessionStatus(sessionID, SessionStatus.Status.CREATED));

                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                    log.warn("CST interrupted while waiting for tunne building attempting anyways", e);
                }

                //attempt to get tunnel information from router service thread
                if (msgQueue.isEmpty()) {
                    log.error("Unable to make tunnel please restart router");
                    clientSock.sendMessage(new SessionStatus(sessionID, SessionStatus.Status.REFUSED));
                    return;
                }

                I2CPMessage routerMsg = msgQueue.remove();


                if (routerMsg.getType() != REQUESTLEASESET) {
                    log.error("Message given to CST is wrong type must be leaseset)");
                    log.debug(routerMsg.toJSONType().getFormattedJSON());
                    return;
                }

                RequestLeaseSet leases = (RequestLeaseSet) routerMsg;
                // ask client to authorize leaseSets
                clientSock.sendMessage(new RequestLeaseSet(sessionID, leases.getLeases()));

                // get authorization
                recvMsg = clientSock.getMessage();

                isTypeBad(recvMsg, CREATELEASESET);

                CreateLeaseSet createLeaseSet = (CreateLeaseSet) recvMsg;
                LeaseSet leaseSet = createLeaseSet.getLeaseSet();
                // this is the private key for elgamal stuff corresponding to public key in
                // leaseset for encryption
                PrivateKey privateKey = createLeaseSet.getPrivateKey();
                //store leaseSet in our netDB and send leaseSet out to neighbors
                netDB.store(leaseSet);
                //send to 3 nearby peers
                for (RouterInfo peer : netDB.getKClosestRouterInfos(leaseSet.getHash(), 3)) {
                    //create store message
                    I2NPHeader store = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, random.nextInt(), System.currentTimeMillis() + 100,
                            new DatabaseStore(leaseSet));
                    //send to nearby peers
                    routerSock.sendMessage(store, peer);
                }
                buildTunnel(clientDestination, router, false); // outbound
                while (true) { // might be a better way to do this that avoids busy waiting
                    // wait until a new message on socket or a new message has arrived from router
                    if (!clientSock.hasMessage() && msgQueue.isEmpty())
                        continue;

                    // deal with client messages
                    if (clientSock.hasMessage()) {
                        recvMsg = clientSock.getMessage();
                        log.debug("Handling message type " + recvMsg.getType());
                        switch (recvMsg.getType()) {
                            case SENDMESSAGE -> {
                                SendMessage send = (SendMessage) recvMsg;

                                Record record = netDB.lookup(send.getDestination().getHash());
                                LeaseSet destLease = (LeaseSet) record;
                                if (destLease == null) {
                                    log.warn("LeaseSet not found for destination "
                                            + Base64.toBase64String(send.getDestination().getHash()));
                                    throw new IllegalStateException("LeaseSet not found for destination ");
                                }
                                // get leases from the leaseset
                                ArrayList<Lease> leasesfromset = destLease.getLeases(); // get the first lease from the
                                                                                        // leaseset
                                // select first lease from the lease set cause we only actually make one
                                Lease lease = leasesfromset.getFirst(); // get the first lease from the leaseset

                                byte[] gatewayHash = lease.getTunnelGW(); // get the router info for the gateway
                                RouterInfo gateway = (RouterInfo) netDB.lookup(gatewayHash); // get the router info for the gateway

                                // REMINDER: WE NEED AN INTERNAL PAYLOAD FOR THE MESSAGE TO SEND TO THE DESTINATION FROM THE SECOND ENDPOINT

                                // realistically... we need to encrypt message with the pub key of the destination
                                // for testing it is plain text cause i love life
                                // god i wish there was a way to highlight this in the code
                                EndpointPayload payload = new EndpointPayload(lease.getTunnelID(),
                                        gateway.getRouterID().getHash(), send.getPayload());

                                // btduwbs it is fine for gateway router to know dest information cause it our pookie :D

                                ConcurrentHashMap<Integer, Tunnel> outboundTunnels = tunnelManager.getOutboundTunnels();
                                // select a random outbound tunnel to send the message through
                                int tunnelID = (int) outboundTunnels.keySet().toArray()[random
                                        .nextInt(outboundTunnels.size())];
                                Tunnel outboundTunnel = tunnelManager.getOutboundTunnel(tunnelID);
                                RouterInfo outboundGateway = outboundTunnel.getGateway(); // get the gateway for the
                                                                                          // outbound tunnel

                                // encrypt the payload
                                payload.encryptPayload(destLease.getEncryptionKey());

                                TunnelDataMessage tunnelDataMessage = new TunnelDataMessage(
                                        outboundTunnel.getGatewayTunnelID(), payload.toJSONType());

                                // send the message to the router
                                I2NPHeader message = new I2NPHeader(I2NPHeader.TYPE.TUNNELDATA, random.nextInt(),
                                        System.currentTimeMillis() + 100, tunnelDataMessage);
                                routerSock.sendMessage(message, outboundGateway);
                            }
                            case DESTLOOKUP -> {
                                DestinationLookup lookup = (DestinationLookup) recvMsg;
                                Destination dest = destLookup(lookup.getHash());
                                clientSock.sendMessage(new DestinationReply(sessionID, dest));
                            }
                            case DESTROYSESSION -> {
                                clientSock.close();
                                routerSock.close();
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

                    if (!msgQueue.isEmpty()) {
                        I2CPMessage message = msgQueue.remove();
                        // todo handle getting message from router and giving it back to the client
                        if (message.getType() != PAYLOADMESSAGE) {
                            log.warn("Bad message from router" + message.toJSONType().getFormattedJSON());
                            continue;
                        }
                        PayloadMessage payload = (PayloadMessage) message;
                        payload.getEncPayload();
                        JSONObject json = null;
                        try {
                            // Decrypt the payload using ElGamal
                            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("ElGamal");
                            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey);
                            byte[] decryptedPayload = cipher.doFinal(payload.getEncPayload());
                            System.err.println(new String(decryptedPayload, StandardCharsets.UTF_8));
                            // Convert the decrypted bytes back to a JSONObject
                            json = JsonIO.readObject(new String(decryptedPayload, StandardCharsets.UTF_8));
                            System.out.println(json.getFormattedJSON());

                        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException
                                 | InvalidKeyException e) {
                            System.err.println("Error decrypting payload: " + e.getMessage());
                            e.printStackTrace();

                        }
                        clientSock.sendMessage(new PayloadMessage(sessionID, 0, json));
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
         * @param destination Destination of the client
         * @param router      RouterInfo of this router
         */
        private void buildTunnel(Destination destination, RouterInfo router, boolean isInbound) {
            // note: client destination is not hard set to endpoint of tunnel
            // it is tacked on to the messge itself so the endpoint knows where to forward

            // hold the records for the tunnel
            ArrayList<TunnelBuild.Record> records = new ArrayList<>();

            // generate a tunnel id for the tunnel
            int tunnelID = random.nextInt(1, Integer.MAX_VALUE);

            // generate random number of hops from 3 to 5
            int numHops = 3; // 3 to 5 hops

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

            // Generate unique tunnel IDs per hop
            int[] hopTunnelIDs = new int[tempPeers.size()];

            for (int i = 0; i < tempPeers.size(); i++) {
                if (i == 0) {
                    hopTunnelIDs[i] = tunnelID; // first hop is the tunnel id
                } else {
                    hopTunnelIDs[i] = random.nextInt(1, Integer.MAX_VALUE); // random id for the rest of the hops
                }
            }

            int sendMessageID = random.nextInt(1, Integer.MAX_VALUE); // unique message id for this message
            long requestTime = System.currentTimeMillis(); // time of request

            ArrayList<TunnelHopInfo> hopInfo = new ArrayList<>(); // this is for the hops in the tunnel

            // create a map of the peers in the tunnel to their router ids
            Tunnel potentialTunnel = new Tunnel();

            for (int i = 0; i < tempPeers.size(); i++) {
                RouterInfo current = tempPeers.get(i);

                byte[] toPeer = Arrays.copyOf(current.getRouterID().getHash(), 16); // only first 16 bytes of the hash
                int receiveTunnel = hopTunnelIDs[i]; // tunnel id for the tunnel

                SecretKey layerKey = generateAESKey(256);
                byte[] layerIv = new byte[16];
                random.nextBytes(layerIv); // generate a random iv for the layer key
                SecretKey ivKey = generateAESKey(256);
                SecretKey replyKey = generateAESKey(256);

                byte[] replyIv = new byte[16];
                random.nextBytes(replyIv);

                potentialTunnel.addTunnelObject(hopTunnelIDs[i], current, replyKey, replyIv); // add the router to the
                                                                                              // tunnel

                ArrayList<TunnelHopInfo> hopInfoInput = null; // this is for the hops in the tunnel

                boolean replyFlag = false; // this is for the hops in the tunnel - they change this later

                TunnelHopInfo hopInfoItem = new TunnelHopInfo(toPeer, layerKey, layerIv, ivKey,
                        receiveTunnel);
                hopInfo.add(hopInfoItem); // add to the front of the list
                // oops i realized this is now wrong cause i flipped the order of the loop hehe

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
                        System.err.println("queue under " + receiveTunnel);
                        tempPeers.set(i, router); // make sure to overwrite for later
                        next = router; // set to client creating request if real for testing set to gateway router
                        msgQueue = new ConcurrentLinkedQueue<>();
                        // currInboundTunnelID = receiveTunnel;
                        clientMessages.put(receiveTunnel, msgQueue);

                    } else {
                        // Forward reply through inbound tunnel gateway
                        LeaseSet leaseSet = (LeaseSet) netDB.lookup(destination.getHash());
                        if (leaseSet == null) {
                            log.warn("LeaseSet not found for destination "
                                    + Base64.toBase64String(destination.getHash()));
                            throw new IllegalStateException("LeaseSet not found for destination ");
                        }
                        ArrayList<Lease> leases = leaseSet.getLeases(); // get the first lease from the leaseset
                        Lease lease = leases.getFirst(); // get the first lease from the leaseset
                        byte[] gatewayHash = lease.getTunnelGW();
                        next = (RouterInfo) netDB.lookup(gatewayHash); // get the router info for the gateway
                        nextTunnel = lease.getTunnelID(); // get the tunnel id for the gateway
                    }
                } else {
                    position = TunnelBuild.Record.TYPE.PARTICIPANT;
                    next = tempPeers.get(i + 1);
                    nextTunnel = hopTunnelIDs[i + 1]; // tunnel id for the next hop
                }
                byte[] nextIdent = next.getHash();

                System.out.println("toPeer created: " + Base64.toBase64String(toPeer));

                TunnelBuild.Record record = new TunnelBuild.Record(
                        toPeer,
                        receiveTunnel,
                        current.getHash(),
                        nextTunnel,
                        nextIdent,
                        layerKey,
                        layerIv,
                        ivKey,
                        replyKey,
                        replyIv,
                        requestTime,
                        sendMessageID,
                        position,
                        hopInfoInput, // only gateway gets this not null
                        replyFlag); // flips to true in response if good

                records.add(record);
            }

            // set the first record hop info to the list
            TunnelBuild.Record firstRecord = records.get(0);
            firstRecord.setHopInfo(hopInfo); // set the hop info for the first record
            // had to do this cause i flipped the loop SORRY!!! i know its clanky

            // save this list of peers to the tunnel manager for easy access later
            if (isInbound) {
                System.out.println("Adding inbound tunnel: " + tunnelID);
                tunnelManager.addInboundTunnel(tunnelID, potentialTunnel);
            } else {
                System.out.println("Adding outbound tunnel: " + tunnelID);
                tunnelManager.addOutboundTunnel(tunnelID, potentialTunnel);
            }

            // save the reply keys of each record
            ArrayList<SecretKey> replyKeys = new ArrayList<>();
            for (int i = 0; i < records.size(); i++) {
                TunnelBuild.Record record = records.get(i);
                replyKeys.add(record.getReplyKey()); // save the reply key for the record
            }

            // encrypt the records for the tunnel build message
            for (int i = 0; i < records.size(); i++) {
                TunnelBuild.Record record = records.get(i);
                record.hybridEncrypt(tempPeers.get(i).getRouterID().getElgamalPublicKey(), record.getReplyKey());
            }

            // OKAY NOW WE AES ENCRYPT WISH ME LUCK!!!!
            // first record is not encrypted with AES, it is the gateway record
            // second record is encrypted with replyKey from the first record
            // third record is encrypted with the reply key from record 2 THEN from record 1
            // and so on

            for (int i = 1; i < records.size(); i++) { // Start from the second record (index 1)
                TunnelBuild.Record currentRecord = records.get(i);

                // Apply layered encryption
                for (int j = i - 1; j >= 0; j--) { // Encrypt using reply keys from all previous records
                    TunnelBuild.Record previousRecord = records.get(j);

                    SecretKey aesKey = replyKeys.get(j); // Get the reply key for the previous record
                    byte[] iv = previousRecord.getReplyIv();

                    System.out.println("Layering encryption with key: " + Base64.toBase64String(aesKey.getEncoded()));
                    System.out.println("Layering encryption with iv: " + Base64.toBase64String(iv));

                    // Encrypt the current record with the replyKey and replyIv from the previous
                    // record
                    currentRecord.layeredEncrypt(aesKey, iv);
                }
            }

            // send tunnel build message to the first peer in the list
            RouterInfo firstPeer = tempPeers.get(0);
            I2NPHeader tunnelBuildMessage = new I2NPHeader(I2NPHeader.TYPE.TUNNELBUILD, random.nextInt(),
                    System.currentTimeMillis() + 100, new TunnelBuild(records));
            I2NPSocket buildSocket = null;
            try {
                System.out.println("Sending tunnel build message to " + firstPeer.getRouterID().getHash());
                System.out.println("the message looks like... " + tunnelBuildMessage.toJSONType().getFormattedJSON());
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
                            System.currentTimeMillis() + 200, new DatabaseLookup(hash, router.getHash()));
                    routerSock.sendMessage(lookup, peer);
                }
                // wait for responses
                try {
                    Thread.sleep(350);
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
