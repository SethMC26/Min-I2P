package common.I2P.tunnels;

import common.I2P.I2NP.EndpointPayload;
import common.I2P.I2NP.I2NPHeader;
import common.I2P.I2NP.I2NPMessage;
import common.I2P.I2NP.TunnelDataMessage;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.transport.I2NPSocket;
import merrimackutil.json.types.JSONObject;

import javax.crypto.SecretKey;
import java.io.IOException;
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
    public TunnelEndpoint(Integer tunnelID, SecretKey tunnelEncryptionKey, SecretKey tunnelIVKey,
            SecretKey replyKey, byte[] replyIV, byte[] replyRouter, Integer replyTunnelID, NetDB netDB) {
        super(TYPE.ENDPOINT, tunnelID, tunnelEncryptionKey, tunnelIVKey, replyKey, replyIV);
        this.replyRouter = replyRouter; // uhhhh
        this.replyTunnelID = replyTunnelID;
        this.netDB = netDB; // Initialize with actual NetDB instance if needed
    }

    @Override
    public void handleMessage(TunnelDataMessage message) throws IOException {
        if (!(message instanceof TunnelDataMessage)) {
            throw new IOException("Expected TunnelDataMessage but received: " + message.getClass().getSimpleName());
        }
        System.out.println("TunnelEndpoint received message: " + message);

        // assume it is an endpoint payload
        EndpointPayload payload = new EndpointPayload(message.getPayload());

        // reminder, the message.getpayload would be another endpoint payload (maybe?)

        TunnelDataMessage tdm = new TunnelDataMessage(payload.getTunnelID(), payload.getJsonObject());
        
         int msgID = new SecureRandom().nextInt();
        //recasting to a new TunnelDataMessage here might be fine or might cause errors - seth
        I2NPHeader header = new I2NPHeader(I2NPHeader.TYPE.TUNNELDATA, msgID, System.currentTimeMillis() + 100, tdm);

        I2NPSocket socket = new I2NPSocket();
        RouterInfo routerInfo = (RouterInfo) netDB.lookup(payload.getRouterID().getHash());
        socket.sendMessage(header, routerInfo);

    }
}
