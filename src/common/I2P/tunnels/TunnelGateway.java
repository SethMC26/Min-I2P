package common.I2P.tunnels;

import common.I2P.I2NP.DatabaseLookup;
import common.I2P.I2NP.I2NPHeader;
import common.I2P.I2NP.I2NPMessage;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.RouterInfo;
import common.transport.I2NPSocket;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Random;

/**
 * This class represents a gateway in a tunnel
 */
public class TunnelGateway extends TunnelObject{
    /**
     * what router is the next one in the path
     */
    private byte[] nextHop;
    /**
     * The tunnel ID on the next hop
     */
    private Integer nextTunnelID;

    /**
     * RouterInfo of the router this gateway is on
     */
    private RouterInfo routerInfo;

    /**
     * Create TunnelGateway
     * @param tunnelID Integer ID of tunnel
     * @param tunnelEncryptionKey AES key for encrypting messages
     * @param tunnelIVKey AES key for IV encryption
     * @param replyKey AES key for encrypting reply
     * @param replyIV byte[] reply IV
     * @param nextHop RouterID hash for next Router in path
     * @param nextTunnelID Integer TunnelID on next hop
     */
    public TunnelGateway(Integer tunnelID, SecretKey tunnelEncryptionKey, SecretKey tunnelIVKey, SecretKey replyKey,
                            byte[] replyIV, byte[] nextHop, Integer nextTunnelID, RouterInfo routerInfo) {
        super(TYPE.GATEWAY, tunnelID, tunnelEncryptionKey, tunnelIVKey, replyKey, replyIV);
        this.nextHop = nextHop;
        this.nextTunnelID = nextTunnelID;
        this.routerInfo = routerInfo;
    }

    @Override
    public void handleMessage(I2NPMessage message) throws IOException {
        // decrypt the message initially client -> router
        // get router info for each hop in the path
        // recursively encrypt the messaege for each hop in the path with the pk
        // skip for now

        // temporary just forward the message to the next hop
        I2NPHeader encryptedMessage = encryptMessage(message);

        sendToNextHop(encryptedMessage);
    }

    private I2NPHeader encryptMessage(I2NPMessage message) {
        // Implement encryption using tunnelEncryptionKey and tunnelIVKey
        return null; // placeholder
    }

    private void sendToNextHop(I2NPHeader encryptedMessage) {
        try {
            // query net db for next hop router info using the hash (nexthop)
            // create lookup message to send to net db
            DatabaseLookup lookupMessage = new DatabaseLookup(nextHop, routerInfo.getHash());
            Random rand = new Random();
            int msgID = rand.nextInt(Integer.MAX_VALUE);

            I2NPHeader lookupHeader = new I2NPHeader(I2NPHeader.TYPE.DATABASELOOKUP, msgID, System.currentTimeMillis() + 10000, lookupMessage);

            // oh wait fuck we need to send this down a tunnel
            I2NPSocket socket = new I2NPSocket();
            socket.sendMessage(lookupHeader, routerInfo);

            // question will this bypass service thread???
            I2NPHeader recvMessage = socket.getMessage(); // wait for the response
            if (recvMessage.getType() != I2NPHeader.TYPE.DATABASESTORE) {
                System.err.println("Invalid response from next hop: " + recvMessage.getType());
                return;
            }
            //wait uuuuhhhh dont do below we query our own netdb

            // get the router info from the response
            // RouterInfo nextRouterInfo = (RouterInfo) recvMessage.getMessage();

            socket.close();
        } catch (IOException e) {
            System.err.println("Failed to send message to next hop: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
