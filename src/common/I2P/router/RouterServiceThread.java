package common.I2P.router;

import common.I2P.I2NP.*;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.Record;
import common.I2P.NetworkDB.RouterInfo;
import common.transport.I2NPSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.ArrayList;

public class RouterServiceThread implements Runnable{
    private NetDB netDB;
    private I2NPSocket sock;
    private I2NPHeader messageHeader;
    private SecureRandom random;

    private RouterInfo router;
    RouterServiceThread(NetDB networkDatabase,RouterInfo router, I2NPSocket socket, I2NPHeader messageHeader) {
        this.netDB = networkDatabase;
        this.router = router;
        this.sock = socket;
        this.messageHeader = messageHeader;
        this.random = new SecureRandom();
    }

    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        if (!messageHeader.isPayloadValid())
            return; //corrupt message - may want to add response for reliable send in future

        if (messageHeader.getExpiration() > System.currentTimeMillis())
            return; //message has expired throw away


        switch(messageHeader.getType()) {

            case DATABASELOOKUP:
                DatabaseLookup lookup = (DatabaseLookup) messageHeader.getMessage();
                handleLookup(lookup);
                break;
            case DATABASESEARCHREPLY:
                DatabaseSearchReply searchReply = (DatabaseSearchReply) messageHeader.getMessage();

                break;
            case DATABASESTORE:
                DatabaseStore store = (DatabaseStore) messageHeader.getMessage();
                //add Record to our netDB
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
           result = new DatabaseStore(record); //create store message if we found record
        }
        //if no record found send search reply with closest peers
        else {
            //get hashes of closest peers that could have key
            ArrayList<byte[]> closestPeersHashes = new ArrayList<>();
            for (RouterInfo currPeer : netDB.getKClosestRouterInfos(lookup.getKey(), 3)) {
                closestPeersHashes.add(currPeer.getHash());
            }
            result = new DatabaseSearchReply(lookup.getKey(), closestPeersHashes, router.getHash());
        }

        I2NPHeader response = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, messageHeader.getMsgID(),
                System.currentTimeMillis() + 1000, result);

        //send result to peer who requested it
        switch (lookup.getReplyFlag()) {
            case 0 -> {
                try {
                    sock.sendMessage(response);
                }
                catch (IOException e) {
                    System.err.println("could not send message I/O error " + e.getMessage());
                }
            }
            case 1 -> {
                //todo send message on tunnel using tunnelID
                int tunnelID = lookup.getReplyTunnelID();
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
