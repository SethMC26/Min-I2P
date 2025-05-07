package common.I2P.tunnels;

import common.I2P.I2NP.*;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.transport.I2NPSocket;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.SocketException;
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
    public TunnelParticipant(Integer tunnelID, SecretKey tunnelEncryptionKey, byte[] layerIv, SecretKey tunnelIVKey,
                             SecretKey replyKey, byte[] replyIV, byte[] nextHop, Integer nextTunnelID, NetDB netDB) {
        super(TYPE.PARTICIPANT, tunnelID, tunnelEncryptionKey, layerIv, tunnelIVKey, replyKey, replyIV);
        this.nextHop = nextHop;
        this.nextTunnelID = nextTunnelID;
        this.netDB = netDB;
    }

    @Override
    public void handleMessage(I2NPMessage message) {
        // decrypt the message initially client -> router
        // get router info for each hop in the path
        // recursively encrypt the messaege for each hop in the path with the pk
        // skip for now

        if (message instanceof TunnelDataMessage) {
            handleTunnelDataMessage((TunnelDataMessage) message);
        } else if (message instanceof TunnelBuildReplyMessage) {
            handleTunnelBuildReplyMessage((TunnelBuildReplyMessage) message);
        }

    }

    private void handleTunnelBuildReplyMessage(TunnelBuildReplyMessage message) {
        message.setNextTunnel(nextTunnelID); // set the tunnel ID for the next hop

        int msgID = new SecureRandom().nextInt(); // generate a random message ID, with secure random
        I2NPHeader header = new I2NPHeader(I2NPHeader.TYPE.TUNNELBUILDREPLY, msgID, System.currentTimeMillis() + 100, message);
        try {
            I2NPSocket socket = new I2NPSocket();
            RouterInfo nextRouter = (RouterInfo) netDB.lookup(nextHop); // this is a dangerous cast could crash here
            socket.sendMessage(header, nextRouter);
            socket.close(); // make sure to close socket
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    private void handleTunnelDataMessage(TunnelDataMessage message) {
        message.setTunnelID(nextTunnelID); // set the tunnel ID for the next hop
        // decryptMessage(message); // decrypt the message
        sendDataToNextHop(new TunnelDataMessage(message.getTunnelID(), decryptMessage(message).toJSONType()));
    }

    private EndpointPayload decryptMessage(TunnelDataMessage message) {
        EndpointPayload payload = new EndpointPayload(message.getPayload());
        payload.layerDecrypt(tunnelEncryptionKey, layerIv); // decrypt the payload
        return payload; // return the decrypted payload
    }

    private void sendDataToNextHop(I2NPMessage encryptedMessage) {
        try {
            int msgID = new SecureRandom().nextInt(); // generate a random message ID, with secure random

            // create header for the message
            I2NPHeader header = new I2NPHeader(I2NPHeader.TYPE.TUNNELDATA, msgID, System.currentTimeMillis() + 100,
                    encryptedMessage);
            I2NPSocket socket = new I2NPSocket();
            RouterInfo nextRouter = (RouterInfo) netDB.lookup(nextHop); // this is a dangerous cast could crash here
                                                                        // should be fixed -seth
            socket.sendMessage(header, nextRouter);

            socket.close(); // make sure to close socket
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
