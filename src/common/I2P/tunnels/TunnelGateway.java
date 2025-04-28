package common.I2P.tunnels;

import common.I2P.I2NP.I2NPMessage;
import common.I2P.IDs.RouterID;

import javax.crypto.SecretKey;
import java.io.IOException;

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
                            byte[] replyIV, byte[] nextHop, Integer nextTunnelID) {
        super(TYPE.GATEWAY, tunnelID, tunnelEncryptionKey, tunnelIVKey, replyKey, replyIV);
        this.nextHop = nextHop;
        this.nextTunnelID = nextTunnelID;
    }

    @Override
    public void handleMessage(I2NPMessage message) throws IOException {
        // encrypt
        // send message to next hop
        throw new RuntimeException("Not implemented");
    }

    private byte[] encryptMessage(I2NPMessage message) {
        // Implement encryption using tunnelEncryptionKey and tunnelIVKey
        return new byte[0]; // placeholder
    }

    private void sendToNextHop(byte[] encryptedMessage) {
        // Send to next router in tunnel
    }
}
