package common.I2P.tunnels;

import common.I2P.I2NP.I2NPHeader;
import common.I2P.I2NP.I2NPMessage;
import common.I2P.I2NP.TunnelDataMessage;
import common.I2P.I2NP.TunnelHopInfo;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.Logger;
import common.transport.I2NPSocket;
import merrimackutil.json.types.JSONObject;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.ArrayList;

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
    public void handleMessage(TunnelDataMessage message) {
        // decrypt the message initially client -> router
        // get router info for each hop in the path
        // recursively encrypt the messaege for each hop in the path with the pk
        // skip for now

        // temporary just forward the message to the next hop
        //I2NPMessage encryptedMessage = encryptMessage(message);
        message.setTunnelID(nextTunnelID); // set the tunnel ID for the next hop

        sendToNextHop(message);
    }

    private I2NPMessage encryptMessage(I2NPMessage message) {
        I2NPMessage currentPayload = message;
        return currentPayload;
    }

    private void sendToNextHop(I2NPMessage encryptedMessage) {
        try {
            int msgID = new SecureRandom().nextInt(); // generate a random message ID, with secure random

            
            // create header for the message
            I2NPHeader header = new I2NPHeader(I2NPHeader.TYPE.TUNNELDATA, msgID, System.currentTimeMillis() + 10, encryptedMessage);
            I2NPSocket socket = new I2NPSocket();
            RouterInfo nextRouter = (RouterInfo) netDB.lookup(nextHop); //this is a dangerous cast could crash here should be fixed -seth
            socket.sendMessage(header, nextRouter);

            socket.close(); //make sure to close socket
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
