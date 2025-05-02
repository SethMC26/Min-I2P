package common.I2P.tunnels;

import common.I2P.I2NP.DatabaseLookup;
import common.I2P.I2NP.I2NPHeader;
import common.I2P.I2NP.I2NPMessage;
import common.I2P.I2NP.TunnelDataMessage;
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
        I2NPMessage encryptedMessage = encryptMessage(message);

        sendToNextHop(encryptedMessage);
    }

    private I2NPMessage encryptMessage(I2NPMessage message) {
        I2NPMessage currentPayload = message;
    
        // Reverse the hop list: we wrap from last hop to first
        for (int i = hops.size() - 1; i >= 0; i--) {
            TunnelHopInfo hop = hops.get(i);
            Integer tunnelId = hop.getSendTunnelId(); // use the tunnel ID for this hop
    
            // Wrap the current payload in a new TunnelDataMessage for this hop
            currentPayload = new TunnelDataMessage(tunnelId, currentPayload);
        }
    
        // Outer layer goes to the first hop in the tunnel
        return currentPayload;
    }

    private void sendToNextHop(I2NPMessage encryptedMessage) {
        try {
            Random random = new Random();
            int msgID = random.nextInt(0xFFFF); // generate a random message ID
            // create header for the message
            I2NPHeader header = new I2NPHeader(I2NPHeader.TYPE.TUNNELDATA, msgID, System.currentTimeMillis() + 10, encryptedMessage);
            I2NPSocket socket = new I2NPSocket();
            RouterInfo nextRouter = (RouterInfo) netDB.lookup(nextHop);
            socket.sendMessage(header, nextRouter);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
