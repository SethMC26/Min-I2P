package common.I2P.tunnels;

import common.I2P.I2NP.*;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.transport.I2NPSocket;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * This class represents a gateway in a tunnel
 */
public class TunnelGateway extends TunnelObject {
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
     * 
     * @param tunnelID            Integer ID of tunnel
     * @param tunnelEncryptionKey AES key for encrypting messages
     * @param tunnelIVKey         AES key for IV encryption
     * @param replyKey            AES key for encrypting reply
     * @param replyIV             byte[] reply IV
     * @param nextHop             RouterID hash for next Router in path
     * @param nextTunnelID        Integer TunnelID on next hop
     */
    public TunnelGateway(Integer tunnelID, SecretKey tunnelEncryptionKey, byte[] layerIv, SecretKey tunnelIVKey, SecretKey replyKey,
            byte[] replyIV, byte[] nextHop, Integer nextTunnelID, RouterInfo routerInfo, ArrayList<TunnelHopInfo> hops,
            NetDB netDB) {
        super(TYPE.GATEWAY, tunnelID, tunnelEncryptionKey, layerIv, tunnelIVKey, replyKey, replyIV);
        this.nextHop = nextHop;
        this.nextTunnelID = nextTunnelID;
        this.routerInfo = routerInfo;
        this.hops = hops;
        this.netDB = netDB;
    }

    @Override
    public void handleMessage(I2NPMessage message) {
        // decrypt the message initially client -> router
        // get router info for each hop in the path
        // recursively encrypt the messaege for each hop in the path with the pk
        // skip for now

        if (message instanceof TunnelDataMessage) {
            System.out.println("TunnelGateway received TunnelDataMessage");
            handleTunnelDataMessage((TunnelDataMessage) message);
        } else if (message instanceof TunnelBuildReplyMessage) {
            System.out.println("TunnelGateway received TunnelBuildReplyMessage");
            handleTunnelBuildReplyMessage((TunnelBuildReplyMessage) message);
        } else {
            System.out.println("TunnelGateway received unknown message type: " + message.getClass().getSimpleName());
            return;
        }

        // temporary just forward the message to the next hop
        // I2NPMessage encryptedMessage = encryptMessage(message);
        // message.setTunnelID(nextTunnelID); // set the tunnel ID for the next hop

        // sendToNextHop(message);
    }

    private void handleTunnelBuildReplyMessage(TunnelBuildReplyMessage message) {
        System.out.println("this is what the gateway thinks the next tunnel id is: " + nextTunnelID);
        message.setNextTunnel(nextTunnelID); // set the tunnel ID for the next hop

        int msgID = new SecureRandom().nextInt(); // generate a random message ID, with secure random
        I2NPHeader header = new I2NPHeader(I2NPHeader.TYPE.TUNNELBUILDREPLY, msgID, System.currentTimeMillis() + 100, message);
        try {
            I2NPSocket socket = new I2NPSocket();
            RouterInfo nextRouter = (RouterInfo) netDB.lookup(nextHop); // this is a dangerous cast could crash here
            socket.sendMessage(header, nextRouter);
            socket.close(); // make sure to close socket
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    private void handleTunnelDataMessage(TunnelDataMessage message) {
        message.setTunnelID(nextTunnelID); // set the tunnel ID for the next hop
        System.out.println("TunnelGateway received TunnelDataMessage: " + message.toJSONType().getFormattedJSON());
        //encryptMessage(message); // encrypt the message for the next hop
        //fuck it lets do ti
        sendDataToNextHop(new TunnelDataMessage(message.getTunnelID(), encryptMessage(message).toJSONType()));
    }

    private EndpointPayload encryptMessage(TunnelDataMessage message) {
        // assuming it is an endpoint payload cause man f type checking
        EndpointPayload payload = new EndpointPayload(message.getPayload());
        // we now iterate through each hopin the tunnel, right to left, skipping this one, and layer encrypt with their layer keys
        // can access this info from the hops list
        System.out.println("this is the size of the hops list: " + hops.size());
        for (int i = hops.size() - 1; i >= 1; i--) { // skips gateway
            TunnelHopInfo hop = hops.get(i);
            SecretKey layerKey = hop.getLayerKey();
            byte[] layerIV = hop.getLayerIv();
            if (i == hops.size() - 1) { // first iteration
                payload.firstLayerEncrypt(layerKey, layerIV); // different variables hence why we gotta use this
            } else {
                payload.layerEncrypt(layerKey, layerIV); // perform layer encryption with the hop's layer key
            }
        }
        return payload;
    }

    private void sendDataToNextHop(I2NPMessage encryptedMessage) {
        try {
            int msgID = new SecureRandom().nextInt(); // generate a random message ID, with secure random

            // create header for the message
            I2NPHeader header = new I2NPHeader(I2NPHeader.TYPE.TUNNELDATA, msgID, System.currentTimeMillis() + 100,
                    encryptedMessage);
            I2NPSocket socket = new I2NPSocket();
            RouterInfo nextRouter = (RouterInfo) netDB.lookup(nextHop); // this is a dangerous cast could crash here
                                                                        // should be fixed -seth
            socket.sendMessage(header, nextRouter);

            socket.close(); // make sure to close socket
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
