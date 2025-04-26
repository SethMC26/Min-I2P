package common.I2P.tunnels;

import common.I2P.I2NP.I2NPMessage;
import common.I2P.IDs.RouterID;

import javax.crypto.SecretKey;
import java.io.IOException;

/**
 * This class represents a Participant in a Tunnel
 */
public class TunnelParticipant extends Tunnel{
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
    public TunnelParticipant(Integer tunnelID, SecretKey tunnelEncryptionKey, SecretKey tunnelIVKey,
                                SecretKey replyKey, byte[] replyIV, RouterID nextHop, Integer nextTunnelID) {
        super(TYPE.PARTICIPANT, tunnelID, tunnelEncryptionKey, tunnelIVKey, replyKey, replyIV);
        this.nextHop = nextHop;
        this.nextTunnelID = nextTunnelID;
    }

    @Override
    public void handleMessage(I2NPMessage message) throws IOException {
        // decrypt the message
        // remove the outside layer
        // send to next hop
        throw new RuntimeException("Not implemented");
    }

    private byte[] decryptLayer(I2NPMessage message) {
        // AES decrypt one layer
        return new byte[0]; // placeholder
    }

    private I2NPMessage rebuildMessage(byte[] decrypted) {
        // Reconstruct message
        return null;
    }

    private void sendToNextHop(I2NPMessage message) {
        // Send to next hop
    }
}
