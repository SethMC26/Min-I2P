package common.I2P.tunnels;

import common.I2P.I2NP.I2NPMessage;
import common.I2P.IDs.RouterID;

import javax.crypto.SecretKey;
import java.io.IOException;

/**
 * This class represents a TunnelEndpoint
 */
public class TunnelEndpoint extends Tunnel{
    private RouterID replyRouter;
    private Integer replyTunnelID;
    /**
     * Create Tunnel Endpoint
     * @param tunnelID Integer ID of tunnel - could be null if Inbound
     * @param tunnelEncryptionKey AES key for encrypting messages
     * @param tunnelIVKey AES key for IV encryption
     * @param replyKey AES key for encrypting reply
     * @param replyIV byte[] reply IV - could be bull if inbound
     * @param replyRouter RouterID of the inbound gateway of the tunnel to send the reply through - could be null if inbound
     * @param replyTunnelID Integer TunnelID on next hop
     */
    public TunnelEndpoint(Integer tunnelID, SecretKey tunnelEncryptionKey, SecretKey tunnelIVKey, SecretKey replyKey,
                          byte[] replyIV, RouterID replyRouter, Integer replyTunnelID) {
        super(TYPE.ENDPOINT, tunnelID, tunnelEncryptionKey, tunnelIVKey, replyKey, replyIV);
        this.replyRouter = replyRouter;
        this.replyTunnelID = replyTunnelID;
    }

    @Override
    void handleMessage(I2NPMessage message) throws IOException {
        throw new RuntimeException("Not implemented");
    }
}
