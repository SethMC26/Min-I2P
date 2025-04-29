package common.I2P.router;

import common.I2P.I2NP.*;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.Record;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.tunnels.TunnelEndpoint;
import common.I2P.tunnels.TunnelGateway;
import common.I2P.tunnels.TunnelManager;
import common.I2P.tunnels.TunnelObject;
import common.I2P.tunnels.TunnelParticipant;
import common.Logger;
import common.transport.I2NPSocket;
import java.util.Base64;

import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
                DatabaseLookup lookup = (DatabaseLookup) recievedMessage.getMessage();
                log.debug("Handling lookup message ");
                handleLookup(lookup);
                break;
            case DATABASESEARCHREPLY:
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
                if (handleTunnelBuildReplyMessage(tunnelBuildReply)) {
                    log.debug("TunnelBuildReply message is valid, tunnel manager preserved");
                } else {
                    log.debug("Bad reply message, removing from TunnelManager");
                }
                break;
            default:
                throw new RuntimeException("Bad message type " + recievedMessage.getType()); // should never hit case in
                                                                                             // prod
        }
    }

    private boolean handleTunnelBuildReplyMessage(TunnelBuildReplyMessage tunnelBuildReply) {
        // logically know which ones are dummy data and skip over them implement later
        // as this does not exist
        // for now we just iterate through all the records
        // check if each record has 0x0 at the end of the bytes
        // we do not need to decrypt the data as i havent implemented this

        for (TunnelBuild.Record record : tunnelBuildReply.getRecords()) {
            byte[] encData = record.getEncData();
            if (encData[encData.length - 1] == 0x0) {
                log.info("Record ends with 0x0, processing further...");
            } else {
                log.debug("Record does not end with 0x0, bad reply...");
                // tunnelManager.removeInboundTunnel(tunnelID); or something ahhhHHHHH!!!
                // we need inbound outbound specificication
                return false;
            }
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

                    byte[] replyBlock = createReplyBlock(record);
                    record.setEncData(replyBlock);

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

                } catch (Exception e) {
                    log.error("Error processing TunnelBuild record: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return;
    }

    private void handleEndpointBehavior(TunnelBuild tunnelBuild, common.I2P.I2NP.TunnelBuild.Record record) {
        TunnelBuildReplyMessage replyMessage = new TunnelBuildReplyMessage(tunnelBuild.getRecords());

        System.out.println("TunnelBuildReply message created: " + replyMessage.toJSONType().getFormattedJSON());
        // query netdb for router info of next hop
        RouterInfo nextRouter = (RouterInfo) netDB.lookup(record.getOurIdent()); // gets router info record right?
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

    private byte[] createReplyBlock(common.I2P.I2NP.TunnelBuild.Record record) {
        try {
            // Generate random padding
            byte[] padding = new byte[495];
            random.nextBytes(padding);

            // Set the reply status byte
            byte replyStatus = 0x0; // 0x0 means agree to participate

            // Combine padding and status byte
            byte[] replyData = new byte[528];
            System.arraycopy(padding, 0, replyData, 32, padding.length);
            replyData[527] = replyStatus;

            // Compute SHA-256 hash of bytes 32-527
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(Arrays.copyOfRange(replyData, 32, 528));

            // Place the hash in bytes 0-31
            System.arraycopy(hash, 0, replyData, 0, hash.length);

            // Encrypt the reply block with AES-256-CBC
            // Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            // SecretKey aesKey = record.getReplyKey();
            // IvParameterSpec iv = new IvParameterSpec(record.getReplyIv());
            // cipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);

            // return cipher.doFinal(replyData);
            return replyData; // Placeholder for actual encryption
        } catch (Exception e) {
            log.error("Error creating reply block: " + e.getMessage());
            throw new RuntimeException("Failed to create reply block", e);
        }
    }

    private boolean isTimestampValid(long requestTime) {
        long currentTime = System.currentTimeMillis() / 1000;
        long requestHour = requestTime / 3600;
        long currentHour = currentTime / 3600;

        // Allow for clock skew (5 minutes ahead, 65 minutes behind)
        return requestHour == currentHour || requestHour == currentHour - 1;
    }

    // in reality encrypt it here and spit out bytes for encdata
    // private byte[] createReplyBlock(TunnelBuild.Record record) throws Exception {
    // return record.;
    // }

    private void addTunnelToManager(TunnelBuild.Record record) {
        if (record.getPosition() == TunnelBuild.Record.TYPE.GATEWAY) {
            TunnelGateway tunnelGateway = new TunnelGateway(
                    record.getReceiveTunnel(),
                    record.getLayerKey(),
                    record.getIvKey(),
                    record.getReplyKey(),
                    record.getReplyIv(),
                    record.getNextIdent(),
                    record.getNextTunnel());
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
                    router.getPort());
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
                    record.getNextTunnel());
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
                findPeerRecordForReply(10, lookup.getFromHash());
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
            for (RouterInfo currPeer : netDB.getKClosestRouterInfos(lookup.getKey(), 2)) {
                closestPeersHashes.add(currPeer.getHash());
            }
            // create message to send to requesting router
            result = new I2NPHeader(I2NPHeader.TYPE.DATABASESEARCHREPLY, recievedMessage.getMsgID(),
                    System.currentTimeMillis() + 5,
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
                        I2NPHeader lookupMessage = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, random.nextInt(),
                                System.currentTimeMillis() + 10,
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
        // if we do not have record we use floodfill record by sending it to 2
        // friends(routers)
        // lets send store to 2 nearest neighbors
        // turn off flood fill for now to test netDB(network is so small that everyone
        // would store everything)
        /*
         * if (netDB.lookup(store.getRecord().getHash()) == null) {
         * ArrayList<RouterInfo> closestPeers=
         * netDB.getKClosestRouterInfos(store.getKey(), 2);
         * I2NPSocket floodSock = null;
         * try {
         * //create socket to send store request to peers
         * floodSock = new I2NPSocket();
         * }
         * catch (SocketException e) {
         * System.err.println("Could not connect to peers " + e.getMessage());
         * }
         * 
         * //send store request to nearest peers
         * for (RouterInfo peer : closestPeers) {
         * log.trace("Sending flood store to peer: " +
         * Base64.toBase64String(peer.getHash()));
         * //create send store request, we will say store request valid for 3 seconds
         * I2NPHeader peerMSG = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE,
         * random.nextInt(),
         * System.currentTimeMillis() + 100, new DatabaseStore(store.getRecord()));
         * //send message to peer
         * try {
         * floodSock.sendMessage(peerMSG, peer);
         * }
         * catch (IOException e) {
         * System.err.println("I/O exception occured " + e.getMessage());
         * }
         * }
         * //close socket if created
         * if (floodSock != null )
         * floodSock.close();
         * }
         */
        // add Record to our netDB
        netDB.store(store.getRecord());

        if (store.getReplyToken() > 0) {
            DeliveryStatus deliveryStatus = new DeliveryStatus(recievedMessage.getMsgID(), System.currentTimeMillis());
            int tunnelID = store.getReplyTunnelID();
            byte[] replyGatewayHash = store.getReplyGateway();
            // todo send response on tunnelD

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
        ArrayList<RouterInfo> closestPeers = netDB.getKClosestRouterInfos(fromHash, 2);
        I2NPSocket peerSock = null;

        try {
            peerSock = new I2NPSocket();
            long expiration = System.currentTimeMillis() + 5;
            for (RouterInfo peer : closestPeers) {
                // avoid recursively sending messages to ourself could happen if someone sends
                // us
                if (peer.getPort() == router.getPort())
                    continue;

                log.trace("Asking peer port: " + peer.getPort() + " to find: "
                        + Base64.getEncoder().encodeToString(fromHash));
                I2NPHeader peerLookup = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, random.nextInt(),
                        expiration, new DatabaseLookup(fromHash, router.getHash()));
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
}
