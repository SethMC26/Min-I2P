package common.I2P.router;

import common.I2P.I2NP.*;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.Record;
import common.I2P.NetworkDB.RouterInfo;
import common.Logger;
import common.transport.I2NPSocket;
import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Handle incoming I2NP messages
 */
public class RouterServiceThread implements Runnable{
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
     * Create thread to handle router I2NP message
     * @param networkDatabase Network database of router
     * @param router RouterInfo of this router
     * @param recievedMessage I2NP message received
     */
    public RouterServiceThread(NetDB networkDatabase, RouterInfo router, I2NPHeader recievedMessage) {
        this.netDB = networkDatabase;
        this.router = router;
        this.recievedMessage = recievedMessage;
        this.random = new SecureRandom();
        this.log= Logger.getInstance();
    }

    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        if (!recievedMessage.isPayloadValid()){
            log.warn("Received corrupted payload");
            return;//corrupt message - may want to add response for reliable send in future
        }

        if (recievedMessage.getExpiration() < System.currentTimeMillis()) {
            log.warn("Received expired message");
            return; //message has expired throw away
        }
        log.trace("Received message " + recievedMessage.toJSONType().getFormattedJSON());

        switch(recievedMessage.getType()) {
            case DATABASELOOKUP:
                DatabaseLookup lookup = (DatabaseLookup) recievedMessage.getMessage();
                log.trace("Handling lookup message ");
                handleLookup(lookup);
                break;
            case DATABASESEARCHREPLY:
                DatabaseSearchReply searchReply = (DatabaseSearchReply) recievedMessage.getMessage();
                log.trace("Handling search reply ");
                handleSearchReply(searchReply);
                break;
            case DATABASESTORE:
                DatabaseStore store = (DatabaseStore) recievedMessage.getMessage();
                //add Record to our netDB
                log.trace("Handling store message ");
                handleStore(store);
                break;
            case DELIVERYSTATUS:
                DeliveryStatus status = (DeliveryStatus) recievedMessage.getMessage();
                //todo implement delivery status
                break;
            case TUNNELBUILD:
                TunnelBuild tunnelBuild = (TunnelBuild) recievedMessage.getMessage();
                //todo implement tunnels
                break;
            case TUNNELBUILDREPLY:
                //todo implement tunnels 1
                break;
            default:
                throw new RuntimeException("Bad message type " + recievedMessage.getType()); // should never hit case in prod
        }
    }
    private void handleLookup(DatabaseLookup lookup) {
        //result message
        I2NPHeader result;
        //try to find record
        Record record = netDB.lookup(lookup.getKey());

        if (record != null) {
            log.trace("Found Record in NetDB");
            //create store message to send to requesting router
            result = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, recievedMessage.getMsgID(),
                    System.currentTimeMillis() + 1010,
                    new DatabaseStore(record)); //create store message if we found record
        }
        //if no record found send search reply with closest peers
        else {
            log.trace("Record not found sending nearest neighbors");
            //get hashes of closest peers that could have key
            ArrayList<byte[]> closestPeersHashes = new ArrayList<>();
            for (RouterInfo currPeer : netDB.getKClosestRouterInfos(lookup.getKey(), 3)) {
                closestPeersHashes.add(currPeer.getHash());
            }
            //create message to send to requesting router
            result = new I2NPHeader(I2NPHeader.TYPE.DATABASESEARCHREPLY, recievedMessage.getMsgID(),
                    System.currentTimeMillis() + 1010,
                    new DatabaseSearchReply(lookup.getKey(), closestPeersHashes, router.getHash()));
        }

        log.trace("Response message is " + result.toJSONType().getFormattedJSON());

        //send result to peer who requested it
        switch (lookup.getReplyFlag()) {
            case 0 -> {
                Record requestRouter = netDB.lookup(lookup.getFromHash());
                //this means node is bootstrapping or verifying store
                if (Arrays.equals(lookup.getFromHash(), lookup.getKey()) &&
                        (requestRouter != null && (requestRouter.getRecordType() == Record.RecordType.ROUTERINFO))) {
                    log.trace("Bootstrapping verification");
                    //if successfully(stored in db) and is info(bootstrapping)
                    try {
                        //send our info to bootstrap peer
                        I2NPSocket sock = new I2NPSocket();
                        I2NPHeader storeMsg = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, random.nextInt(),
                                System.currentTimeMillis() + 100, new DatabaseStore(router));
                        sock.sendMessage(storeMsg, (RouterInfo) requestRouter);
                        System.err.println("CCCC");

                        return;
                    } catch (SocketException e) {
                        log.warn("Could not connect to peer " + Base64.toBase64String(requestRouter.getHash()));
                        log.warn(e.getMessage());
                        return;
                    }
                    catch (IOException e) {
                        log.warn("error sending to bootstrap peer" + e.getMessage());
                    }
                }
                //check if we dont know where to send message to
                if (requestRouter == null) {
                    //attempt to find peer for reply we will wait 1 seconds
                    findPeerRecordForReply(10, lookup.getFromHash());
                    requestRouter = netDB.lookup(lookup.getFromHash());
                    //if we still do not know give up
                    if (requestRouter == null) {
                        log.warn("Could not find who sent lookup even after asking peers fromHash: " + Base64.toBase64String(lookup.getFromHash()));
                        return;
                    }
                }
                if (requestRouter.getRecordType() == Record.RecordType.LEASESET) {
                    //todo handle this case, question for sam can we use leaseSets to send a message
                    return;
                }

                //lets send our lookup response back to peer who requested it
                //assuming here it is RouterInfo might need to change later once leaseSets are implemented
                RouterInfo requestRouterInfo = (RouterInfo) requestRouter;
                I2NPSocket respondSock = null;
                try {
                    respondSock = new I2NPSocket();
                    respondSock.sendMessage(result, requestRouterInfo);
                    respondSock.close();
                }
                catch (IOException e) {
                    if (respondSock != null)
                        respondSock.close();
                    System.err.println("could not send message I/O error " + e.getMessage());
                }
            }
            case 1 -> {
                //todo send message on tunnel using tunnelID
            }
        }
    }

    private void handleSearchReply(DatabaseSearchReply searchReply) {
        //query closest peers to see if they have the hash
        ArrayList<byte[]> peerHash = searchReply.getPeerHashes();
        I2NPSocket peerSocket = null;
        try {
            peerSocket = new I2NPSocket();
        } catch (SocketException e) {
            log.warn("RST: Could not connect on socket " + e.getMessage());
            return;
        }
        //send message to each hash
        for (byte[] hash : peerHash) {
            I2NPHeader lookupMessage = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, random.nextInt(),
                    System.currentTimeMillis() + 60, new DatabaseLookup(searchReply.getKey(), router.getHash()));
            Record peerRecord = netDB.lookup(hash);

            if (peerRecord == null) {
                //attempt to find peer lets wait 100 ms for each  reply
                findPeerRecordForReply(10, hash);
            }

            //check if we found peer if so send lookup message
            peerRecord = netDB.lookup(hash);
            //if still null let's just try the next peer
            if (peerRecord == null) {
                continue;
            }
            switch(peerRecord.getRecordType()) {
                case ROUTERINFO -> {
                    //send lookup request to peer
                    RouterInfo peerRouterInfo = (RouterInfo) peerRecord;
                    try {
                        peerSocket.sendMessage(lookupMessage, peerRouterInfo);
                    } catch (IOException e) {
                        log.warn("Could not connect/send message to peer" + e.getMessage());
                        peerSocket.close();
                    }
                }
                case LEASESET -> {
                    //todo add support for leasesets
                }
            }
        }
        peerSocket.close();
    }
    private void handleStore(DatabaseStore store) {
        //if we do not have record we use floodfill record by sending it to 2 friends(routers)
        //lets send store to 2 nearest neighbors
        /*
        if (netDB.lookup(store.getRecord().getHash()) == null) {
            ArrayList<RouterInfo> closestPeers= netDB.getKClosestRouterInfos(store.getKey(), 2);
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
                log.trace("Sending flood store to peer: " + Base64.toBase64String(peer.getHash()));
                //create send store request, we will say store request valid for 3 seconds
                I2NPHeader peerMSG = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, random.nextInt(),
                        System.currentTimeMillis() + 100, new DatabaseStore(store.getRecord()));
                //send message to peer
                try {
                    floodSock.sendMessage(peerMSG, peer);
                }
                catch (IOException e) {
                    System.err.println("I/O exception occured " + e.getMessage());
                }
            }
            //close socket if created
            if (floodSock != null )
                floodSock.close();
        }
        */
        //add Record to our netDB
        netDB.store(store.getRecord());

        if (store.getReplyToken() > 0) {
            DeliveryStatus deliveryStatus = new DeliveryStatus(recievedMessage.getMsgID(), System.currentTimeMillis());
            int tunnelID = store.getReplyTunnelID();
            byte[] replyGatewayHash = store.getReplyGateway();
            //todo send response on tunnelD

        }

    }

    /**
     * Attempt to find peer to reply to them, we will do this by sending lookups to close routers, we will wait some time
     * to allow network to search for record then we will try again
     * @param msToWait milliseconds we want to wait for reply to come in from routers we know about
     * @param fromHash Hash of peer we need information about
     */
    private void findPeerRecordForReply(int msToWait, byte[] fromHash) {
        //we will ask two of our buddies to see if we could find info to send to information back to this router
        ArrayList<RouterInfo> closestPeers = netDB.getKClosestRouterInfos(fromHash, 2);
        I2NPSocket peerSock = null;

        try {
            peerSock = new I2NPSocket();

            for (RouterInfo peer : closestPeers) {
                //avoid recursively sending messages to ourself
                if (peer.getPort() == router.getPort())
                    continue;

                I2NPHeader peerLookup = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, random.nextInt(),
                        System.currentTimeMillis() + 100, new DatabaseLookup(router.getHash(), fromHash));
                peerSock.sendMessage(peerLookup, peer);
                System.err.println("BBBB" + peer.getPort());

            }

            Thread.sleep(msToWait); // wait for results
            peerSock.close();
        } catch (IOException e) {
            log.warn("Could not connect to peers" + e);
            if (peerSock != null)
                peerSock.close(); //close sock if possible
        } catch (InterruptedException e) {
            log.warn("Sleep was interrupted");
            peerSock.close();
        }
    }
}
