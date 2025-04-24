package common.I2P.NetworkDB;

public class Lease {
    /**
     * Hash of RouterID of the tunnel Gateway
     */
    private byte[] tunnelGW;

    /**
     * Tunnel ID for lease
     */
    private int tunnelID;

    /**
     * Epoch time of expiration
     */
    private long expiration;


}
