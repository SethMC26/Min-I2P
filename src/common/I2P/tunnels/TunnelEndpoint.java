package common.I2P.tunnels;

import common.I2P.I2NP.I2NPMessage;
import common.I2P.I2NP.TunnelDataMessage;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.NetDB;

import javax.crypto.SecretKey;
import java.io.IOException;

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
        this.replyRouter = replyRouter;
        this.replyTunnelID = replyTunnelID;
        this.netDB = netDB; // Initialize with actual NetDB instance if needed
    }

    @Override
    public void handleMessage(I2NPMessage message) throws IOException {
        if (!(message instanceof TunnelDataMessage)) {
            throw new IOException("Expected TunnelDataMessage but received: " + message.getClass().getSimpleName());
        }

        TunnelDataMessage tdm = (TunnelDataMessage) message;
        I2NPMessage innerPayload = tdm.getPayload();
        deliver(innerPayload);
    }

    private void deliver(I2NPMessage message) {
        // For now just print or log it
        System.out.println("TunnelEndpoint received final message: " + message);

        // deliver to local client or hand off to next tunnel
    }
}
