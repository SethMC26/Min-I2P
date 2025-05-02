package common.I2P.I2NP;

import java.security.PublicKey;

import javax.crypto.SecretKey;


public class TunnelHopInfo {
    byte[] routerHash; // Router identity hash 16 byte for toPeer
    // PublicKey publicKey; // ElGamal key (used during build) do we need this?
    SecretKey layerKey; // AES key for that hop
    SecretKey ivKey; // AES IV key
    int sendTunnelId; // Tunnel ID for the next hop

    /**
     * Constructor for TunnelHopInfo
     * @param routerHash
     * @param layerKey
     * @param ivKey
     * @param sendTunnelId
     */
    public TunnelHopInfo(byte[] routerHash, SecretKey layerKey, SecretKey ivKey, int sendTunnelId) {
        this.routerHash = routerHash;
        // this.publicKey = publicKey;
        this.layerKey = layerKey;
        this.ivKey = ivKey;
        this.sendTunnelId = sendTunnelId;
    }

    public byte[] getRouterHash() {
        return routerHash;
    }

    // public PublicKey getPublicKey() {
    //     return publicKey;
    // }

    public SecretKey getLayerKey() {
        return layerKey;
    }

    public SecretKey getIvKey() {
        return ivKey;
    }

    public int getSendTunnelId() {
        return sendTunnelId;
    }

}
