package common.I2P.tunnels;

import common.I2P.I2NP.I2NPMessage;
import common.I2P.RouterID;
import common.transport.SOCK;

import javax.crypto.SecretKey;

/**
 * This class represents a gateway in a tunnel
 */
public class TunnelGateway extends Tunnel{
    /**
     * what router is the next one in the path
     */
    private RouterID nextHop;
    /**
     * The tunnel ID on the next hop
     */
    private Integer nextTunnelID;

    /**
     * Create TunnelGateway
     * @param tunnelID Integer ID of tunnel
     * @param tunnelEncryptionKey AES key for encrypting messages
     * @param tunnelIVKey AES key for IV encryption
     * @param replyKey AES key for encrypting reply
     * @param replyIV byte[] reply IV
     * @param nextHop RouterID for next Router in path
     * @param nextTunnelID Integer TunnelID on next hop
     */
    public TunnelGateway(Integer tunnelID, SecretKey tunnelEncryptionKey, SecretKey tunnelIVKey, SecretKey replyKey,
                            byte[] replyIV, RouterID nextHop, Integer nextTunnelID) {
        super(TYPE.GATEWAY, tunnelID, tunnelEncryptionKey, tunnelIVKey, replyKey, replyIV);
        this.nextHop = nextHop;
        this.nextTunnelID = nextTunnelID;
    }

    @Override
    boolean handleMessage(I2NPMessage message, SOCK socket) {
        throw new RuntimeException("Not implemented");
    }
}
