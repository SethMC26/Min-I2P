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

    /**
     * TunnelManager constructor
     */
    public TunnelManager() {
        inboundTunnels = new ConcurrentHashMap<>();
        outboundTunnels = new ConcurrentHashMap<>();
        tunnelObjects = new ConcurrentHashMap<>();
    }

    /**
     * Add an inbound tunnel to the list of inbound tunnels
     * @param tunnelID
     * @param tunnel
     */
    public void addInboundTunnel(Integer tunnelID, Tunnel tunnel) {
        inboundTunnels.put(tunnelID, tunnel);
    }

    /**
     * Add an outbound tunnel to the list of outbound tunnels
     * @param tunnelID
     * @param tunnel
     */
    public void addOutboundTunnel(Integer tunnelID, Tunnel tunnel) {
        outboundTunnels.put(tunnelID, tunnel);
    }

    /**
     * Get the tunnel ID of the tunnel that contains the given tunnel object ID
     * This is also gateway tunnel ID
     * 
     * @param tunnelObjectID
     * @return the tunnel ID of the tunnel that contains the given tunnel object ID, or -1 if not found
     */
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

    public ConcurrentHashMap<Integer, Tunnel> getOutboundTunnels() {
        return outboundTunnels;
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
