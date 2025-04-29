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
import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
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
                // todo implement tunnels 1
                break;
            default:
                throw new RuntimeException("Bad message type " + recievedMessage.getType()); // should never hit case in
                                                                                             // prod
        }
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
                if (record.getPosition() == TunnelBuild.Record.TYPE.GATEWAY) {
                    // create new tunnel gateway object
                    TunnelGateway tunnelGateway = new TunnelGateway(record.getReceiveTunnel(),
                            record.getLayerKey(), record.getIvKey(), record.getReplyKey(),
                            record.getReplyIv(), record.getNextIdent(), record.getNextTunnel());

                    // add gateway to tunnel manager
                    tunnelManager.addTunnelObject(record.getReceiveTunnel(), tunnelGateway);
                } else if (record.getPosition() == TunnelBuild.Record.TYPE.ENDPOINT) {
                    // create new tunnel endpoint object
                    TunnelEndpoint tunnelEndpoint = new TunnelEndpoint(record.getReceiveTunnel(),
                            record.getLayerKey(), record.getIvKey(), record.getReplyKey(),
                            record.getReplyIv(), router.getHash(), router.getPort());
                    // add endpoint to tunnel manager
                    tunnelManager.addTunnelObject(record.getReceiveTunnel(), tunnelEndpoint);

                } else if (record.getPosition() == TunnelBuild.Record.TYPE.PARTICIPANT) {
                    // create new tunnel participant object
                    TunnelParticipant tunnelParticipant = new TunnelParticipant(record.getReceiveTunnel(),
                            record.getLayerKey(), record.getIvKey(), record.getReplyKey(),
                            record.getReplyIv(), record.getNextIdent(), record.getNextTunnel());
                    // add participant to tunnel manager
                    tunnelManager.addTunnelObject(record.getReceiveTunnel(), tunnelParticipant);

                    // in final practice we decrypt here but im stupid

                    // forward to next hop
                    // Wrap the TunnelBuild in an I2NPHeader
                    I2NPHeader tunnelBuildHeader = new I2NPHeader(
                            I2NPHeader.TYPE.TUNNELBUILD,
                            new Random().nextInt(), // Unique message ID
                            System.currentTimeMillis() + 1000, // Expiration time
                            tunnelBuild);

                    // Create a socket and send the message
                    I2NPSocket socket = null;
                    try {
                        socket = new I2NPSocket();
                        // Define and assign targetRouter before using it
                        RouterInfo targetRouter = netDB.lookup(record.getNextIdent())
                                .getRecordType() == Record.RecordType.ROUTERINFO
                                        ? (RouterInfo) netDB.lookup(record.getNextIdent())
                                        : null;
                        if (targetRouter == null) {
                            log.warn("Target router not found for next hop: "
                                    + Base64.toBase64String(record.getNextIdent()));
                            return;
                        }
                        socket.sendMessage(tunnelBuildHeader, targetRouter);
                    } catch (SocketException e) {
                        log.error("SocketException occurred: " + e.getMessage());
                    } catch (IOException e) {
                        log.error("IOException occurred while sending message: " + e.getMessage());
                    } finally {
                        if (socket != null) {
                            socket.close();
                        }
                    }

                } else {

                }
            }
        }
        return;
    }

    private void handleLookup(DatabaseLookup lookup) {
        // send reply directly to router requesting it
        Record requestRouter = netDB.lookup(lookup.getFromHash());
        if (lookup.getReplyFlag() == 0) {
            // check if we dont know where to send message to
            if (requestRouter == null) {
                log.trace("Could not find peer: " + Base64.toBase64String(lookup.getFromHash()));
                // attempt to find peer for reply we will wait 10 milli seconds
                findPeerRecordForReply(10, lookup.getFromHash());
                requestRouter = netDB.lookup(lookup.getFromHash());
                // if we still do not know give up
                if (requestRouter == null) {
                    log.warn("Could not find who sent lookup even after asking peers fromHash: "
                            + Base64.toBase64String(lookup.getFromHash()));
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

                log.trace("Asking peer port: " + peer.getPort() + " to find: " + Base64.toBase64String(fromHash));
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
