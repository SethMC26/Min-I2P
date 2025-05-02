package common.I2P.tunnels;

import common.I2P.I2NP.DatabaseLookup;
import common.I2P.I2NP.I2NPHeader;
import common.I2P.I2NP.I2NPMessage;
import common.I2P.I2NP.TunnelHopInfo;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.transport.I2NPSocket;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
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
     * List of hops in the tunnel
     */
    private ArrayList<TunnelHopInfo> hops;

    private NetDB netDB;

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
                            byte[] replyIV, byte[] nextHop, Integer nextTunnelID, RouterInfo routerInfo, ArrayList<TunnelHopInfo> hops, NetDB netDB) {
        super(TYPE.GATEWAY, tunnelID, tunnelEncryptionKey, tunnelIVKey, replyKey, replyIV);
        this.nextHop = nextHop;
        this.nextTunnelID = nextTunnelID;
        this.routerInfo = routerInfo;
        this.hops = hops;
        this.netDB = netDB;
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
        I2NPHeader encryptedMessage = null;
        Random random = new Random();
            int messageID = random.nextInt(1000);
        I2NPHeader currentHeader = new I2NPHeader(I2NPHeader.TYPE.TUNNELBUILD, messageID, System.currentTimeMillis() + 10, message);

        // Iterate through the hops in reverse order
        for (int i = hops.size() - 1; i >= 0; i--) {
            TunnelHopInfo hop = hops.get(i);
            // AHHH WE NEED TO STORE NEXT HOP INFO YOU DUMBASSSSSSSS

            // Build a tunnel header for the current hop
            encryptedMessage = new I2NPHeader(I2NPHeader.TYPE.TUNNELBUILD, messageID, System.currentTimeMillis() + 10, currentHeader);

            // Set the current header to the newly created header for the next iteration
            currentHeader = encryptedMessage;
        }
        return encryptedMessage; // placeholder
    }

    private void sendToNextHop(I2NPHeader encryptedMessage) {
        try {
            I2NPSocket socket = new I2NPSocket();
            RouterInfo nextRouter = (RouterInfo) netDB.lookup(nextHop);
            socket.sendMessage(encryptedMessage, nextRouter);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
