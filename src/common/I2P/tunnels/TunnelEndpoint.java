package common.I2P.tunnels;

import common.I2P.I2NP.*;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.transport.I2NPSocket;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.SocketException;
import java.security.SecureRandom;

/**
 * This class represents a TunnelEndpoint
 */
public class TunnelEndpoint extends TunnelObject {
    private byte[] replyRouter;
    private Integer replyTunnelID;
    private NetDB netDB;

    /**
     * Create Tunnel Endpoint
     * 
     * @param tunnelID            Integer ID of tunnel - could be null if Inbound
     * @param tunnelEncryptionKey AES key for encrypting messages
     * @param tunnelIVKey         AES key for IV encryption
     * @param replyKey            AES key for encrypting reply
     * @param replyIV             byte[] reply IV - could be bull if inbound
     * @param replyRouter         RouterID hash of the inbound gateway of the tunnel
     *                            to send the reply through
     * @param replyTunnelID       Integer TunnelID on next hop
     */
    public TunnelEndpoint(Integer tunnelID, SecretKey tunnelEncryptionKey, byte[] layerIv, SecretKey tunnelIVKey,
            SecretKey replyKey, byte[] replyIV, byte[] replyRouter, Integer replyTunnelID, NetDB netDB) {
        super(TYPE.ENDPOINT, tunnelID, tunnelEncryptionKey, layerIv, tunnelIVKey, replyKey, replyIV);
        this.replyRouter = replyRouter; // uhhhh
        this.replyTunnelID = replyTunnelID;
        this.netDB = netDB; // Initialize with actual NetDB instance if needed
    }

    @Override
    public void handleMessage(I2NPMessage message) throws IOException {

        if (message instanceof TunnelDataMessage) {
            handleTunnelDataMessage((TunnelDataMessage) message);
        } else if (message instanceof TunnelBuildReplyMessage) {
            handleTunnelBuildReplyMessage((TunnelBuildReplyMessage) message);
        } else {
            // Handle other message types if necessary
        }
        //System.out.println("TunnelEndpoint received message: " + message);

    }

    private void handleTunnelBuildReplyMessage(TunnelBuildReplyMessage message) {
        //System.out.println("TunnelEndpoint received TunnelBuildReplyMessage: " + message.toJSONType().getFormattedJSON());
        //System.out.println("Seth how do i process this im ascarerhjkaahsdjk");
       // System.out.println("Reminder to self: make build reply message go to the router durrrrr");
    }

    private void handleTunnelDataMessage(TunnelDataMessage message) {
        System.out.println("TunnelEndpoint received TunnelDataMessage1: " + message.toJSONType().getFormattedJSON());
        // assume it is an endpoint payload
        EndpointPayload payload = new EndpointPayload(message.getPayload());
        System.out.println("TunnelEndpoint received TunnelDataMessage: " + payload.toJSONType().getFormattedJSON());

        payload.finalLayerDecrypt(tunnelEncryptionKey, layerIv); // different values so we gotta use this

        // reminder, the message.getpayload would be another endpoint payload (maybe?)

        TunnelDataMessage tdm = new TunnelDataMessage(payload.getTunnelID(), payload.toJSONType());
        System.out.println("TunnelEndpoint received TunnelDataMessage2: " + tdm.toJSONType().getFormattedJSON());
        int msgID = new SecureRandom().nextInt();
        // recasting to a new TunnelDataMessage here might be fine or might cause errors
        // - seth
        I2NPHeader header = new I2NPHeader(I2NPHeader.TYPE.TUNNELDATA, msgID, System.currentTimeMillis() + 100, tdm);

        I2NPSocket socket;
        try {
            socket = new I2NPSocket();
            RouterInfo routerInfo = (RouterInfo) netDB.lookup(payload.getRouterID());
            socket.sendMessage(header, routerInfo);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public byte[] getIV() {
        return super.layerIv;
    }

    public SecretKey getLayerKey() {
        return tunnelEncryptionKey;
    }
}
