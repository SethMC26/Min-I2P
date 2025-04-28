package common.I2P.tunnels;

import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import common.I2P.IDs.RouterID;

public class TunnelManager {
    private ConcurrentHashMap<Integer, TunnelObject> tunnelObjects;

    public TunnelManager() {
        tunnelObjects = new ConcurrentHashMap<>();
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
}
