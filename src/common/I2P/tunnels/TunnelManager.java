package common.I2P.tunnels;

import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import common.I2P.IDs.RouterID;

public class TunnelManager {
    // tunnel lists for inbound and outbound tunnels
    private ConcurrentHashMap<Integer, Tunnel> inboundTunnels;
    private ConcurrentHashMap<Integer, Tunnel> outboundTunnels;

    // tunnel objects the router is comprised of
    private ConcurrentHashMap<Integer, TunnelObject> tunnelObjects;

    public TunnelManager() {
        inboundTunnels = new ConcurrentHashMap<>();
        outboundTunnels = new ConcurrentHashMap<>();
        tunnelObjects = new ConcurrentHashMap<>();
    }

    public void addInboundTunnel(Integer tunnelID, Tunnel tunnel) {
        inboundTunnels.put(tunnelID, tunnel);
    }

    public void addOutboundTunnel(Integer tunnelID, Tunnel tunnel) {
        outboundTunnels.put(tunnelID, tunnel);
    }

    public int findAssociatedTunnel(int tunnelObjectID) {
        // return the tunnelID of the tunnel that contains the tunnelObjectID
        for (Integer tunnelID : inboundTunnels.keySet()) {
            Tunnel tunnel = inboundTunnels.get(tunnelID);
            if (tunnel.getTunnelObject(tunnelObjectID) != null) {
                return tunnelID;
            }
        }
        return -1; // not found
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

    public void addTunnelObject(Integer tunnelID, TunnelObject tunnelObject) {
        tunnelObjects.put(tunnelID, tunnelObject);
    }

    public TunnelObject getTunnelObject(Integer tunnelID) {
        return tunnelObjects.get(tunnelID);
    }

    public void removeTunnelObject(Integer tunnelID) {
        tunnelObjects.remove(tunnelID);
    }

    public ConcurrentHashMap<Integer, TunnelObject> getTunnelObjects() {
        return tunnelObjects;
    }

}
