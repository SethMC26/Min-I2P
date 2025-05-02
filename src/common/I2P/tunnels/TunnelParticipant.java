package common.I2P.tunnels;

import common.I2P.I2NP.I2NPHeader;
import common.I2P.I2NP.I2NPMessage;
import common.I2P.I2NP.TunnelDataMessage;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.transport.I2NPSocket;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Random;

/**
 * This class represents a Participant in a Tunnel
 */
public class TunnelParticipant extends TunnelObject{
    /**
     * what router is the next one in the path
     */
    private byte[] nextHop;
    /**
     * The tunnel ID on the next hop
     */
    private Integer nextTunnelID;

    /**
     * Networkd database for this router
     */
    private NetDB netDB;

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
                             SecretKey replyKey, byte[] replyIV, byte[] nextHop, Integer nextTunnelID, NetDB netDB) {
        super(TYPE.PARTICIPANT, tunnelID, tunnelEncryptionKey, tunnelIVKey, replyKey, replyIV);
        this.nextHop = nextHop;
        this.nextTunnelID = nextTunnelID;
        this.netDB = netDB;
    }

    @Override
    public void handleMessage(I2NPMessage message) throws IOException {
        if (!(message instanceof TunnelDataMessage)) {
            throw new IOException("Expected TunnelDataMessage but received: " + message.getClass().getSimpleName());
        }

        TunnelDataMessage tdm = (TunnelDataMessage) message;
        I2NPMessage innerPayload = tdm.getPayload();
        sendToNextHop(innerPayload);
    }

    private void sendToNextHop(I2NPMessage message) throws IOException {
        Random random = new Random();
        int msgID = random.nextInt(0xFFFF);
        I2NPHeader header = new I2NPHeader(I2NPHeader.TYPE.TUNNELDATA, msgID, System.currentTimeMillis(), message);

        I2NPSocket socket = new I2NPSocket();
        RouterInfo nextRouter = (RouterInfo) netDB.lookup(nextHop);
        socket.sendMessage(header, nextRouter);
    }
}
