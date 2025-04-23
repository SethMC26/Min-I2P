package common.I2P.tunnels;

import common.I2P.I2NP.I2NPMessage;
import common.transport.SOCK;

import javax.crypto.SecretKey;

/**
 * Abstract class for all Tunnels
 */
public abstract class Tunnel {
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
     * @param tunnelIVKey AES key for IV encryption
     * @param replyKey AES key for encrypting reply
     * @param replyIV byte[] reply IV
     */
    protected Tunnel(TYPE type,Integer tunnelID, SecretKey tunnelEncryptionKey, SecretKey tunnelIVKey, SecretKey replyKey, byte[] replyIV ) {
        this.type = type;
        this.tunnelID = tunnelID;
        this.tunnelEncryptionKey = tunnelEncryptionKey;
        this.tunnelIVKey = tunnelIVKey;
        this.replyKey = replyKey;
        this.replyIV = replyIV;
    }

   abstract boolean handleMessage(I2NPMessage message, SOCK socket);

}
