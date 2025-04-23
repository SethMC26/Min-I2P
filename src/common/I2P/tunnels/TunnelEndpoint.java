package common.I2P.tunnels;

import common.I2P.I2NP.I2NPMessage;
import common.I2P.RouterID;
import common.transport.SOCK;

import javax.crypto.SecretKey;

/**
 * This class represents a TunnelEndpoint
 */
public class TunnelEndpoint extends Tunnel{
    private RouterID replyRouter;
    private Integer replyTunnelID;
    /**
     * Create TunnelGateway
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
    boolean handleMessage(I2NPMessage message, SOCK socket) {
        throw new RuntimeException("Not implemented");
    }
}
