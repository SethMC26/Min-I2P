package common.I2P.tunnels;

import common.I2P.I2NP.I2NPMessage;
import common.I2P.I2NP.TunnelDataMessage;

import javax.crypto.SecretKey;
import java.io.IOException;

/**
 * Abstract class for all Tunnels
 */
public abstract class TunnelObject {
    /**
     * TYPE is type of Tunnel {@code {GATEWAY,PARTICIPANT,ENDPOINT}}
     */
    public enum TYPE {
        GATEWAY,
        PARTICIPANT,
        ENDPOINT,
    }

    /**
     * Type of tunnel
     */
    protected TYPE type;
    /**
     * an AES private key for encrypting messages and instructions to the next hop
     */
    protected SecretKey tunnelEncryptionKey;
    /**
     * an AES IV for encrypting messages and instructions to the next hop
     */
    protected byte[] layerIv;
    /**
     * an AES private key for double-encrypting the IV to the next hop
     */
    protected SecretKey tunnelIVKey;
    /**
     * an AES public key for encrypting the reply to the tunnel build request
     */
    protected SecretKey replyKey;
    /**
     * the IV for encrypting the reply to the tunnel build request
     */
    protected byte[] replyIV;
    /**
     * Integer of tunnel id
     */
    protected Integer tunnelID;

    /**
     * Create Abstract tunnel with necessary fields
     * @param type Type of tunnel {@code {GATEWAY,PARTICIPANT,ENDPOINT}}
     * @param tunnelID Integer ID of tunnel
     * @param tunnelEncryptionKey AES key for encrypting messages
     * @param layerIv byte[] IV for encrypting messages
     * @param tunnelIVKey AES key for IV encryption
     * @param replyKey AES key for encrypting reply
     * @param replyIV byte[] reply IV
     */
    protected TunnelObject(TYPE type,Integer tunnelID, SecretKey tunnelEncryptionKey, byte[] layerIv, SecretKey tunnelIVKey, SecretKey replyKey, byte[] replyIV ) {
        this.type = type;
        this.tunnelID = tunnelID;
        this.tunnelEncryptionKey = tunnelEncryptionKey;
        this.layerIv = layerIv;
        this.tunnelIVKey = tunnelIVKey;
        this.replyKey = replyKey;
        this.replyIV = replyIV;
    }

    /**
     * Handles I2NP Message in tunnel
     * @param message I2NP message received
     * @throws IOException Throws if error occurs while sending
     */
   public abstract void handleMessage(I2NPMessage message) throws IOException;

}
