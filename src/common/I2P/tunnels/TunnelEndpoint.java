package common.I2P.tunnels;

import common.I2P.I2NP.I2NPMessage;
import common.I2P.IDs.RouterID;

import javax.crypto.SecretKey;
import java.io.IOException;

/**
 * This class represents a TunnelEndpoint
 */
public class TunnelEndpoint extends TunnelObject{
    private byte[] replyRouter;
    private Integer replyTunnelID;
    /**
     * Create Tunnel Endpoint
     * @param tunnelID Integer ID of tunnel - could be null if Inbound
     * @param tunnelEncryptionKey AES key for encrypting messages
     * @param tunnelIVKey AES key for IV encryption
     * @param replyKey AES key for encrypting reply
     * @param replyIV byte[] reply IV - could be bull if inbound
     * @param replyRouter RouterID hash of the inbound gateway of the tunnel to send the reply through
     * @param replyTunnelID Integer TunnelID on next hop
     */
    public TunnelEndpoint(Integer tunnelID, SecretKey tunnelEncryptionKey, SecretKey tunnelIVKey, SecretKey replyKey,
                          byte[] replyIV, byte[] replyRouter, Integer replyTunnelID) {
        super(TYPE.ENDPOINT, tunnelID, tunnelEncryptionKey, tunnelIVKey, replyKey, replyIV);
        this.replyRouter = replyRouter;
        this.replyTunnelID = replyTunnelID;
    }

    @Override
    public void handleMessage(I2NPMessage message) throws IOException {
        // decrypt the message
        // deliver to local destination or inbound tunnel gateway
        throw new RuntimeException("Not implemented");
    }

    private byte[] decryptCompletely(I2NPMessage message) {
        // Final decryption step
        return new byte[0]; // placeholder
    }

    private void deliver(byte[] payload) {
        // Hand off to local destination (e.g., client or service) or inbound tunnel gateway
    }
}
