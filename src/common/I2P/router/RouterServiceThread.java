package common.I2P.router;

import common.I2P.I2NP.*;
import common.I2P.IDs.Destination;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.Record;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.NetworkDB.Record.RecordType;
import common.I2P.NetworkDB.Lease;
import common.I2P.NetworkDB.LeaseSet;
import common.I2P.tunnels.TunnelEndpoint;
import common.I2P.tunnels.TunnelGateway;
import common.I2P.tunnels.TunnelManager;
import common.I2P.tunnels.TunnelObject;
import common.I2P.tunnels.TunnelParticipant;
import common.Logger;
import common.transport.I2NPSocket;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.SocketException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Random;

/**
 * Handle incoming I2NP messages
 */
public class RouterServiceThread implements Runnable {
    /**
     * Network database
     */
    private NetDB netDB;
    /**
     * Received I2NP message
     */
    private I2NPHeader recievedMessage;
    /**
     * Source of randomness mainly for message ids
     */
    private SecureRandom random;
    /**
     * This Router's info
     */
    private RouterInfo router;
    /**
     * Logger
     */
    private Logger log;
    /**
     * Is this router a flood fill router
     */
    private boolean isFloodFill;

    /**
     * TunnelManager for this router
     */
    private TunnelManager tunnelManager;

    /**
     * Create thread to handle router I2NP message
     * 
     * @param networkDatabase Network database of router
     * @param router          RouterInfo of this router
     * @param recievedMessage I2NP message received
     */
    public RouterServiceThread(NetDB networkDatabase, RouterInfo router, I2NPHeader recievedMessage,
            TunnelManager tunnelManager) {
        this.netDB = networkDatabase;
        this.router = router;
        this.recievedMessage = recievedMessage;
        this.random = new SecureRandom();
        this.log = Logger.getInstance();
        this.isFloodFill = false;
    }

    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        if (!recievedMessage.isPayloadValid()) {
            log.warn("Received corrupted payload");
            return;// corrupt message - may want to add response for reliable send in future
        }

        if (recievedMessage.getExpiration() < System.currentTimeMillis()) {
            log.warn("Received expired message");
            return; // message has expired throw away
        }
        log.trace("Received message " + recievedMessage.toJSONType().getFormattedJSON());

        switch (recievedMessage.getType()) {
            case DATABASELOOKUP:
                //To avoid trivial DDOS attacks let make sure no one sets the expiration very high for recursive searches
                if (recievedMessage.getExpiration() > System.currentTimeMillis() + 1000 ) {
                    log.warn("Received message expiration is too high ignoring");
                    return;
                }
                //handle lookup
                DatabaseLookup lookup = (DatabaseLookup) recievedMessage.getMessage();
                log.debug("Handling lookup message ");
                handleLookup(lookup);
                break;
            case DATABASESEARCHREPLY:
                //To avoid trivial DDOS attacks let make sure no one sets the expiration very high for recursive searches
                if (recievedMessage.getExpiration() > System.currentTimeMillis() + 1000 ) {
                    log.warn("Received message expiration is too high ignoring");
                    return;
                }
                //handle search reply
                DatabaseSearchReply searchReply = (DatabaseSearchReply) recievedMessage.getMessage();
                log.debug("Handling search reply ");
                handleSearchReply(searchReply);
                break;
            case DATABASESTORE:
                DatabaseStore store = (DatabaseStore) recievedMessage.getMessage();
                // add Record to our netDB
                log.debug("Handling store message ");
                handleStore(store);
                break;
            case DELIVERYSTATUS:
                DeliveryStatus status = (DeliveryStatus) recievedMessage.getMessage();
                // todo implement delivery status
                break;
            case TUNNELBUILD:
                TunnelBuild tunnelBuild = (TunnelBuild) recievedMessage.getMessage();
                handleTunnelBuildMessage(tunnelBuild);
                break;
            case TUNNELBUILDREPLY:
                TunnelBuildReplyMessage tunnelBuildReply = (TunnelBuildReplyMessage) recievedMessage.getMessage();
                handleTunnelBuildReplyMessage(tunnelBuildReply);
                break;
            case TUNNELDATA:
                TunnelDataMessage tunnelData = (TunnelDataMessage) recievedMessage.getMessage();
                handleTunnelDataMessage(tunnelData);
                break;
            default:
                throw new RuntimeException("Bad message type " + recievedMessage.getType()); // should never hit case in
                                                                                             // prod
        }
    }

    private void handleTunnelDataMessage(TunnelDataMessage tunnelData) {
        // get tunnel id from message
        int tunnelID = tunnelData.getTunnelID();

        // get the tunnel from the tunnel manager
        TunnelObject tunnelObject = tunnelManager.getTunnelObject(tunnelID);
        if (tunnelObject == null) {
            log.warn("Tunnel object not found for tunnel ID: " + tunnelID);
            return; // Tunnel not found, handle error appropriately
        }

        // handle the message in the tunnel object
        try {
            tunnelObject.handleMessage(tunnelData);
        } catch (IOException e) {
            log.error("Error handling TunnelDataMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean handleTunnelBuildReplyMessage(TunnelBuildReplyMessage tunnelBuildReply) {
        // logically know which ones are dummy data and skip over them implement later
        // as this does not exist
        // for now we just iterate through all the records
        // check if each record has 0x0 at the end of the bytes
        // we do not need to decrypt the data as i havent implemented this

        // THIS NEEDS TO BE IN CLIENT IN REALITY FORWARD TO CLIENT 
        // PLEASE SAM CHANGE THIS ITS ONLY FOR TESTING UNTIL WE HAVE CLIENT LEASE SETS
        // SAMMMMMMM - love past sam <3
        // this is only for inbound tunnel
        
        if (tunnelManager.getInboundTunnel(tunnelBuildReply.getTunnelID()) != null) {
            Lease lease = new Lease(router.getRouterID(), tunnelBuildReply.getTunnelID());
            HashSet<Lease> leases = new HashSet<>();
            Destination destination = new Destination(router.getRouterID().getSigningPublicKey());
            // generate random secret key temporarily
            KeyPairGenerator keyPairGenerator;
            try {
                keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            } catch (NoSuchAlgorithmException e) {
                log.error("RSA algorithm not available: " + e.getMessage());
                return false; // Handle the error appropriately
            }
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();
            LeaseSet leaseSet = new LeaseSet(leases, destination, router.getRouterID().getElgamalPublicKey(), privateKey);

            // publish lease set to netDB
            netDB.store(leaseSet);
        }
        
        return true;
    }

    private void handleTunnelBuildMessage(TunnelBuild tunnelBuild) {
        System.out.println("TunnelBuild message received: " + tunnelBuild.toJSONType().getFormattedJSON());

        // iterate through all the records and compare the first 16 bytes of the hash
        // to the toPeer field of the record, if they match we have found the correct
        // record for us

        for (TunnelBuild.Record record : tunnelBuild.getRecords()) {
            byte[] toPeer = Arrays.copyOf(record.getToPeer(), 16); // only first 16 bytes of the hash
            // check if first 16 byte of hash matches our first 16 bytes of hash
            if (Arrays.equals(toPeer, Arrays.copyOf(router.getHash(), 16))) {
                // we have found the correct record for us, now we can send the reply back to
                // the peer
                try {
                    // Decrypt the ElGamal block

                    // Get session key, we skip bloom filter

                    if (!isTimestampValid(record.getRequestTime())) {
                        log.warn("Invalid timestamp in tunnel request. Dropping record.");
                        continue;
                    }
                    
                    // byte[] replyBlock = createReplyBlock(record);
                    // record.setEncData(replyBlock);

                    // temp before enc
                    common.I2P.I2NP.TunnelBuild.Record replyRecord = createReplyBlock(record);
                    record = replyRecord; // replace the record with the reply block

                    // Add the tunnel to the TunnelManager
                    addTunnelToManager(record);

                    // Handle endpoint behavior
                    if (record.getPosition() == TunnelBuild.Record.TYPE.ENDPOINT) {
                        handleEndpointBehavior(tunnelBuild, record);
                    } else {
                        // forward build request to next hop
                        I2NPSocket nextHopSocket = new I2NPSocket();
                        I2NPHeader header = new I2NPHeader(I2NPHeader.TYPE.TUNNELBUILD, random.nextInt(),
                                System.currentTimeMillis() + 100, tunnelBuild);
                        RouterInfo nextRouter = (RouterInfo) netDB.lookup(record.getNextIdent());
                        nextHopSocket.sendMessage(header, nextRouter);
                        nextHopSocket.close();
                    }
                    // note to self - how do we adjust for recursive decryption on reply records?

                } catch (Exception e) {
                    log.error("Error processing TunnelBuild record: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return;
    }

    private void handleEndpointBehavior(TunnelBuild tunnelBuild, common.I2P.I2NP.TunnelBuild.Record record) {
        TunnelBuildReplyMessage replyMessage;
        if (record.getReplyFlag()) { // this is only checking one fucking reply flag please change you dumbass - love sam who just wrote thsi
            replyMessage = new TunnelBuildReplyMessage(record.getReceiveTunnel(), true);
        } else {
            replyMessage = new TunnelBuildReplyMessage(record.getReceiveTunnel(), false);
        }

        System.out.println("TunnelBuildReply message created: " + replyMessage.toJSONType().getFormattedJSON());
        // query netdb for router info of next hop
        RouterInfo nextRouter = (RouterInfo) netDB.lookup(record.getNextIdent()); // FOR THE LOVE OF GOD PLEASE BE SET PROPERLLY
        // realistically this will be fowarded to the client and client will handle lease set publishing
        // for now we have router act as client cause i dont want to set up a fucking client
        // forward message to next hop
        I2NPSocket nextHopSocket = null;
        try {
            nextHopSocket = new I2NPSocket();
            // create new header
            I2NPHeader header = new I2NPHeader(I2NPHeader.TYPE.TUNNELBUILDREPLY, random.nextInt(),
                    System.currentTimeMillis() + 100, replyMessage);
            nextHopSocket.sendMessage(header, nextRouter);
            nextHopSocket.close();
        } catch (IOException e) {
            if (nextHopSocket != null)
                nextHopSocket.close();
            log.error("Error sending TunnelBuildReply message: " + e.getMessage());
        }
    }

    // switch thsi from bytes to record and have sendmsgid be the reply bit yyyyyasudyasukdghasjhdgashjdgha
    private common.I2P.I2NP.TunnelBuild.Record createReplyBlock(common.I2P.I2NP.TunnelBuild.Record record) {
        // go through the record and replace everything with a random value
        // however, make sendmsgid be 0

        // to peer bytes
        Random random = new Random();
        byte[] toPeer = new byte[16];
        random.nextBytes(toPeer);

        // receive tunnel id int
        int receiveTunnel = random.nextInt(1000);

        // our ident bytes
        byte[] ourIdent = new byte[32];
        random.nextBytes(ourIdent);

        // next tunnel id int
        int nextTunnel = random.nextInt(1000);

        // next ident byte
        byte[] nextIdent = new byte[32];
        random.nextBytes(nextIdent);
        
        // layer key secret
        SecretKey layerKey = new SecretKeySpec(new byte[32], "AES");
        random.nextBytes(layerKey.getEncoded());

        // iv key secret
        SecretKey ivKey = new SecretKeySpec(new byte[32], "AES");
        random.nextBytes(ivKey.getEncoded());

        // reply key secret
        SecretKey replyKey = new SecretKeySpec(new byte[32], "AES");
        random.nextBytes(replyKey.getEncoded());
       
        // reply iv bytes
        byte[] replyIv = new byte[16];
        random.nextBytes(replyIv);

        // request time long
        long requestTime = System.currentTimeMillis() / 1000; // epoch time in seconds
        // send msg id int
        int sendMsgID = 0; // set to 0 for successful reply

        // pick random type
        TunnelBuild.Record.TYPE type = TunnelBuild.Record.TYPE.PARTICIPANT; // this is general enough

        // reply flag set to true here
        // this is also temp plain text
        TunnelBuild.Record replyRecord = new TunnelBuild.Record(toPeer, receiveTunnel, ourIdent, nextTunnel,
                nextIdent, layerKey, ivKey, replyKey, replyIv, requestTime, sendMsgID, type, null, true);

        // we need to encrypt this but for now return the record
        // like this should return bytes in the future
        // new Record(toPeer, encData);

        return replyRecord;
    }

    private boolean isTimestampValid(long requestTime) {
        long currentTime = System.currentTimeMillis() / 1000;
        long requestHour = requestTime / 3600;
        long currentHour = currentTime / 3600;

        // Allow for clock skew (5 minutes ahead, 65 minutes behind)
        return requestHour == currentHour || requestHour == currentHour - 1;
    }

    private void addTunnelToManager(TunnelBuild.Record record) {
        if (record.getPosition() == TunnelBuild.Record.TYPE.GATEWAY) {
            TunnelGateway tunnelGateway = new TunnelGateway(
                    record.getReceiveTunnel(),
                    record.getLayerKey(),
                    record.getIvKey(),
                    record.getReplyKey(),
                    record.getReplyIv(),
                    record.getNextIdent(),
                    record.getNextTunnel(),
                    router,
                    record.getHopInfo(),
                    netDB);
            tunnelManager.addTunnelObject(record.getReceiveTunnel(), tunnelGateway);
            log.info("Added tunnel gateway for tunnel ID: " + record.getReceiveTunnel());
        } else if (record.getPosition() == TunnelBuild.Record.TYPE.ENDPOINT) {
            TunnelEndpoint tunnelEndpoint = new TunnelEndpoint(
                    record.getReceiveTunnel(),
                    record.getLayerKey(),
                    record.getIvKey(),
                    record.getReplyKey(),
                    record.getReplyIv(),
                    router.getHash(),
                    router.getPort(),
                    netDB);
            tunnelManager.addTunnelObject(record.getReceiveTunnel(), tunnelEndpoint);
            log.info("Added tunnel endpoint for tunnel ID: " + record.getReceiveTunnel());
        } else {
            TunnelParticipant tunnelParticipant = new TunnelParticipant(
                    record.getReceiveTunnel(),
                    record.getLayerKey(),
                    record.getIvKey(),
                    record.getReplyKey(),
                    record.getReplyIv(),
                    record.getNextIdent(),
                    record.getNextTunnel(),
                    netDB);
            tunnelManager.addTunnelObject(record.getReceiveTunnel(), tunnelParticipant);
            log.info("Added tunnel participant for tunnel ID: " + record.getReceiveTunnel());
        }
    }

    private void handleLookup(DatabaseLookup lookup) {
        // send reply directly to router requesting it
        Record requestRouter = netDB.lookup(lookup.getFromHash());
        if (lookup.getReplyFlag() == 0) {
            // check if we dont know where to send message to
            if (requestRouter == null) {
                log.trace("Could not find peer: " + Base64.getEncoder().encodeToString(lookup.getFromHash()));
                // attempt to find peer for reply we will wait 10 milli seconds
                findPeerRecordForReply(50, lookup.getFromHash());
                requestRouter = netDB.lookup(lookup.getFromHash());
                // if we still do not know give up
                if (requestRouter == null) {
                    log.warn("Could not find who sent lookup even after asking peers fromHash: "
                            + Base64.getEncoder().encodeToString(lookup.getFromHash()));
                    return;
                }
            }
        }
        if (lookup.getReplyFlag() == 1) {
            // todo add sending reply down some tunnel
        }

        if (requestRouter.getRecordType() == Record.RecordType.LEASESET) {
            // todo handle this case, question for sam can we use leaseSets to send a
            // message
            return;
        }
        // result message
        I2NPHeader result;
        // try to find record
        Record record = netDB.lookup(lookup.getKey());

        if (record != null) { // we have record in database

            log.trace("Found Record in NetDB");
            // store record
            DatabaseStore storeData;

            // we will check for a special bootstrap/verification lookup, where a peer is
            // trying to lookup themeself if so
            // we will send back our info instead of requested info
            if (Arrays.equals(lookup.getKey(), lookup.getFromHash())) {
                log.trace("Bootstrapping/verification sending store as response");
                // we will send back our own info in this case
                storeData = new DatabaseStore(router);
            } else {
                // normal store request so we will just add record we found
                storeData = new DatabaseStore(record);
            }

            result = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, recievedMessage.getMsgID(),
                    System.currentTimeMillis() + 100,
                    storeData); // create store message if we found record

        }
        // if no record found send search reply with closest peers
        else {
            log.trace("Record not found sending nearest neighbors");
            // get hashes of closest peers that could have key
            ArrayList<byte[]> closestPeersHashes = new ArrayList<>();
            for (RouterInfo currPeer : netDB.getKClosestRouterInfos(lookup.getKey(), 3)) {
                closestPeersHashes.add(currPeer.getHash());
            }
            // create message to send to requesting router make sure to decrease expiration so search will timeout
            result = new I2NPHeader(I2NPHeader.TYPE.DATABASESEARCHREPLY, recievedMessage.getMsgID(),
                    recievedMessage.getExpiration() - 10,
                    new DatabaseSearchReply(lookup.getKey(), closestPeersHashes, router.getHash()));
        }

        // lets send our lookup response back to peer who requested it
        // assuming here it is RouterInfo might need to change later once leaseSets are
        // implemented
        RouterInfo requestRouterInfo = (RouterInfo) requestRouter;
        I2NPSocket respondSock = null;
        try {
            respondSock = new I2NPSocket();
            respondSock.sendMessage(result, requestRouterInfo);
            respondSock.close();
        } catch (IOException e) {
            if (respondSock != null)
                respondSock.close();
            System.err.println("could not send message I/O error " + e.getMessage());
        }

        log.trace("Response message is " + result.toJSONType().getFormattedJSON());

    }

    private void handleSearchReply(DatabaseSearchReply searchReply) {
        // query closest peers to see if they have the hash
        ArrayList<byte[]> peerHash = searchReply.getPeerHashes();
        I2NPSocket peerSocket = null;
        try {
            peerSocket = new I2NPSocket();
        } catch (SocketException e) {
            log.warn("RST: Could not connect on socket " + e.getMessage());
            return;
        }

        for (byte[] hash : peerHash) {
            Record peerRecord = netDB.lookup(hash);

            if (peerRecord == null) {
                // attempt to find peer lets wait 5 ms for each reply
                findPeerRecordForReply(10, hash);
            }

            // check if we found peer if so send lookup message
            peerRecord = netDB.lookup(hash);
            // if still null let's just try the next peer
            if (peerRecord == null) {
                continue;
            }

            switch (peerRecord.getRecordType()) {
                case ROUTERINFO -> {
                    // send lookup request to peer
                    RouterInfo peerRouterInfo = (RouterInfo) peerRecord;

                    try {
                        //we will decrease expiration so recursive search expires
                        I2NPHeader lookupMessage = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, random.nextInt(),
                                recievedMessage.getExpiration() - 10,
                                new DatabaseLookup(searchReply.getKey(), router.getHash()));
                        peerSocket.sendMessage(lookupMessage, peerRouterInfo);
                    } catch (IOException e) {
                        log.warn("Could not connect/send message to peer" + e.getMessage());
                        peerSocket.close();
                    }
                }
                case LEASESET -> {
                    // todo add support for leasesets
                }
            }
        }
        peerSocket.close();
    }

    private void handleStore(DatabaseStore store) {
        // if we do not have record we use floodfill record by sending it to 3 friends(Routers)
        if (isFloodFill && (netDB.lookup(store.getKey()) == null)) {
            ArrayList<RouterInfo> closestPeers=
                    netDB.getKClosestRouterInfos(store.getKey(), 3);
            I2NPSocket floodSock = null;
            try {
                //create socket to send store request to peers
                floodSock = new I2NPSocket();
            }
            catch (SocketException e) {
                System.err.println("Could not connect to peers " + e.getMessage());
            }

            //send store request to nearest peers
            for (RouterInfo peer : closestPeers) {
                log.trace("Sending flood store to peer: " + peer.getPort());
                //create send store request, we will say store request valid for 100 ms(store request ok to live longer)
                I2NPHeader peerMSG = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, random.nextInt(),
                        System.currentTimeMillis() + 100, new DatabaseStore(store.getRecord()));
                //send message to peer
                try {
                    floodSock.sendMessage(peerMSG, peer);
                }
                catch (IOException e) {
                    log.error("RST: Issue in floodfill connecting to peer ", e);
                }
            }
            //close socket if created
            if (floodSock != null )
                floodSock.close();

        }
        // add Record to our netDB
        netDB.store(store.getRecord());

        if (store.getReplyToken() > 0) {
            DeliveryStatus deliveryStatus = new DeliveryStatus(recievedMessage.getMsgID(), System.currentTimeMillis());
            int tunnelID = store.getReplyTunnelID(); // this is set in setReply but setReply is never called so this is null
            byte[] replyGatewayHash = store.getReplyGateway(); // see prev comment
            try {
                I2NPSocket replySock = new I2NPSocket();
                I2NPHeader replyMessage = new I2NPHeader(I2NPHeader.TYPE.DELIVERYSTATUS, random.nextInt(),
                        System.currentTimeMillis() + 100, deliveryStatus);
                // send delivery status directly to the router, no tunnels (chicken and egg?)
                // uhhhhh i think i can make tunnels before this? maybe? idk
                // well actually were doing a direct query to this router anyways so a direct reply is fine

                
            } catch (SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    /**
     * Attempt to find peer to reply to them, we will do this by sending lookups to
     * close routers, we will wait some time
     * to allow network to search for record then we will try again
     * 
     * @param msToWait milliseconds we want to wait for reply to come in from
     *                 routers we know about
     * @param fromHash Hash of peer we need information about
     */
    private void findPeerRecordForReply(int msToWait, byte[] fromHash) {
        // we will ask two of our buddies to see if we could find info to send to
        // information back to this router
        ArrayList<RouterInfo> closestPeers = netDB.getKClosestRouterInfos(fromHash, 3);
        I2NPSocket peerSock = null;
        log.trace("Going to ask " + closestPeers.size());
        try {
            peerSock = new I2NPSocket();
            for (RouterInfo peer : closestPeers) {

                log.trace("Asking peer port: " + peer.getPort() + " to find: "
                        + Base64.getEncoder().encodeToString(fromHash));
                I2NPHeader peerLookup = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, random.nextInt(),
                        System.currentTimeMillis() + 100, new DatabaseLookup(fromHash, router.getHash()));
                peerSock.sendMessage(peerLookup, peer);
            }

            Thread.sleep(msToWait); // wait for results
            peerSock.close();
        } catch (IOException e) {
            log.warn("Could not connect to peers" + e);
            if (peerSock != null)
                peerSock.close(); // close sock if possible
        } catch (InterruptedException e) {
            log.warn("Sleep was interrupted");
            peerSock.close();
        }
    }

    /**
     * Get the received message - temp method for testing
     * 
     * @return I2NPHeader message received
     */
    public void setReceivedMessage(I2NPHeader mockHeader) {
        this.recievedMessage = mockHeader;
    }

    /**
     * Turn on floodfill algorithm for new store requests to this router
     * @param isFloodFill Boolean to turn floodfill on/off
     */
    public void setFloodFill(boolean isFloodFill) {
        this.isFloodFill = isFloodFill;
    }
}
