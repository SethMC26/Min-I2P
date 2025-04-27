package common.I2P.tunnels;

import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import common.I2P.IDs.RouterID;

public class TunnelManager {
    private ConcurrentHashMap<Integer, Tunnel> inboundTunnels = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Tunnel> outboundTunnels = new ConcurrentHashMap<>();

    public void addInboundTunnel(Integer tunnelID, Tunnel tunnel) {
        inboundTunnels.put(tunnelID, tunnel);
    }

    public void addOutboundTunnel(Integer tunnelID, Tunnel tunnel) {
        outboundTunnels.put(tunnelID, tunnel);
    }

    public Tunnel getInboundTunnel(Integer tunnelID) {
        return inboundTunnels.get(tunnelID);
    }

    public Tunnel getOutboundTunnel(Integer tunnelID) {
        return outboundTunnels.get(tunnelID);
    }

    public void removeInboundTunnel(Integer tunnelID) {
        inboundTunnels.remove(tunnelID);
    }

    public void removeOutboundTunnel(Integer tunnelID) {
        outboundTunnels.remove(tunnelID);
    }
}
