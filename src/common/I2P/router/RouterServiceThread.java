package common.I2P.router;

import common.I2P.I2NP.*;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.Record;
import common.I2P.NetworkDB.RouterInfo;
import common.Logger;
import common.transport.I2NPSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.ArrayList;

public class RouterServiceThread implements Runnable{
    private NetDB netDB;
    private I2NPHeader messageHeader;
    private SecureRandom random;

    private RouterInfo router;
    private Logger log;
    public RouterServiceThread(NetDB networkDatabase, RouterInfo router, I2NPHeader messageHeader) {
        this.netDB = networkDatabase;
        this.router = router;
        this.messageHeader = messageHeader;
        this.random = new SecureRandom();
        this.log= Logger.getInstance();
    }

    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        log.trace("Received message " + messageHeader.toJSONType().getFormattedJSON());
        if (!messageHeader.isPayloadValid()){
            log.warn("Received corrupted payload");
            return;//corrupt message - may want to add response for reliable send in future
        }

        if (messageHeader.getExpiration() < System.currentTimeMillis()) {
            log.warn("Received expired message");
            return; //message has expired throw away
        }

        switch(messageHeader.getType()) {
            case DATABASELOOKUP:
                DatabaseLookup lookup = (DatabaseLookup) messageHeader.getMessage();
                log.trace("Handling lookup message ");
                handleLookup(lookup);
                break;
            case DATABASESEARCHREPLY:
                DatabaseSearchReply searchReply = (DatabaseSearchReply) messageHeader.getMessage();
                log.trace("Handling search reply ");
                break;
            case DATABASESTORE:
                DatabaseStore store = (DatabaseStore) messageHeader.getMessage();
                //add Record to our netDB
                log.trace("Handling store message ");
                handleStore(store);
                break;
            case DELIVERYSTATUS:
                DeliveryStatus status = (DeliveryStatus) messageHeader.getMessage();
                //todo implement delivery status
                break;
            case TUNNELBUILD:
                TunnelBuild tunnelBuild = (TunnelBuild) messageHeader.getMessage();
                //todo implement tunnels
                break;
            case TUNNELBUILDREPLY:
                //todo implement tunnels 1
                break;
            default:
                throw new RuntimeException("Bad message type " + messageHeader.getType()); // should never hit case in prod
        }
    }
    private void handleLookup(DatabaseLookup lookup) {
        //result message
        I2NPMessage result;
        //try to find record
        Record record = netDB.lookup(lookup.getKey());

        if (record != null) {
            log.trace("Found Record in NetDB");
           result = new DatabaseStore(record); //create store message if we found record
        }
        //if no record found send search reply with closest peers
        else {
            log.trace("Record not found sending nearest neighbors");
            //get hashes of closest peers that could have key
            ArrayList<byte[]> closestPeersHashes = new ArrayList<>();
            for (RouterInfo currPeer : netDB.getKClosestRouterInfos(lookup.getKey(), 3)) {
                closestPeersHashes.add(currPeer.getHash());
            }
            result = new DatabaseSearchReply(lookup.getKey(), closestPeersHashes, router.getHash());
        }

        I2NPHeader response = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, messageHeader.getMsgID(),
                System.currentTimeMillis() + 1000, result);

        log.trace("Response message is " + response.toJSONType().getFormattedJSON());

        //send result to peer who requested it
        switch (lookup.getReplyFlag()) {
            case 0 -> {
                Record requestRouter = netDB.lookup(lookup.getFromHash());
                //check if we dont know where to send message to
                if (requestRouter == null) {
                    //we will ask two of our buddies to see if we could find info to send to information back to this router
                    ArrayList<RouterInfo> closestPeers = netDB.getKClosestRouterInfos(lookup.getFromHash(), 2);
                    try {
                        I2NPSocket peerSock = new I2NPSocket();
                        I2NPHeader peerLookup = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, random.nextInt(),
                                System.currentTimeMillis() + 100, new DatabaseLookup(lookup.getFromHash(), router.getHash()));
                        for (RouterInfo peer : closestPeers) {
                            peerSock.connect(new InetSocketAddress(peer.getHost(), peer.getPort()));
                            peerSock.sendMessage(peerLookup);
                            peerSock.disconnect();
                        }

                        Thread.sleep(1000); // lets wait one second for the results
                    } catch (IOException e) {
                        log.warn("Could not connect to peers" + e);
                    } catch (InterruptedException e) {
                        log.warn("Sleep was interrupted");
                    }
                    //try to get peer who requested lookup again
                    requestRouter = netDB.lookup(lookup.getFromHash());
                    //if we still do not know give up
                    if (requestRouter == null) {
                        log.warn("Could not find who sent lookup even after asking peers fromHash: " + lookup.getFromHash());
                        return;
                    }
                }
                if (record.getRecordType() == Record.RecordType.LEASESET) {
                    //todo handle this case, question for sam can we use leaseSets to send a message
                    return;
                }

                //lets send our lookup response back to peer who requested it
                RouterInfo requestRouterInfo = (RouterInfo) requestRouter;
                try {
                    I2NPSocket respondSock = new I2NPSocket();
                    respondSock.connect(new InetSocketAddress(requestRouterInfo.getHost(), requestRouterInfo.getPort()));

                    respondSock.sendMessage(response);
                }
                catch (IOException e) {
                    System.err.println("could not send message I/O error " + e.getMessage());
                }
            }
            case 1 -> {
                //todo send message on tunnel using tunnelID
            }
        }

    }
    private void handleStore(DatabaseStore store) {
        //add Record to our netDB
        netDB.store(store.getRecord());

        if (store.getReplyToken() > 0) {
            DeliveryStatus deliveryStatus = new DeliveryStatus(messageHeader.getMsgID(), System.currentTimeMillis());
            int tunnelID = store.getReplyTunnelID();
            byte[] replyGatewayHash = store.getReplyGateway();
            //todo send response on tunnelD

            //lets send store to 2 nearest neighbors
            ArrayList<RouterInfo> closestPeers= netDB.getKClosestRouterInfos(store.getKey(), 2);

            try {
                //create socket to send store request to peers
                I2NPSocket floodSock = new I2NPSocket();
                //create send store request, we will say store request valid for 3 seconds
                I2NPHeader peerMSG = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, random.nextInt(),
                        System.currentTimeMillis() + 3000, new DatabaseStore(store.getRecord()));
                //send store request to nearest peers
                for (RouterInfo peer : closestPeers) {
                    //connect to peer to send
                    floodSock.connect(new InetSocketAddress(peer.getHost(), peer.getPort()));
                    //send message to peer
                    floodSock.sendMessage(peerMSG);
                    //disconnect from peer
                    floodSock.disconnect();
                }
            } catch (SocketException e) {
                System.err.println("Could not connect to peers " + e.getMessage());
            } catch (IOException e) {
                System.err.println("I/O exception occured " + e.getMessage());
            }
        }
    }
}
