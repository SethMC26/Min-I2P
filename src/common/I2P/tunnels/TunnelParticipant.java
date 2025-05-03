package common.I2P.tunnels;

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
        JSONObject innerPayload = tdm.getPayload();
        sendToNextHop(innerPayload);
    }

    private void sendToNextHop(JSONObject message) throws IOException {
        int msgID = new SecureRandom().nextInt();
        //recasting to a new TunnelDataMessage here might be fine or might cause errors - seth
        I2NPHeader header = new I2NPHeader(I2NPHeader.TYPE.TUNNELDATA, msgID, System.currentTimeMillis(),new TunnelDataMessage(message));

        I2NPSocket socket = new I2NPSocket();
        RouterInfo nextRouter = (RouterInfo) netDB.lookup(nextHop); //this is a dangerous cast we could crash here -seth
        socket.sendMessage(header, nextRouter);
        socket.close();
    }
}
