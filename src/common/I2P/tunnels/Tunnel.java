package common.I2P.tunnels;

import java.util.ArrayList;
import java.util.HashMap;

import org.bouncycastle.jcajce.provider.asymmetric.mldsa.MLDSAKeyFactorySpi.Hash;

import common.I2P.NetworkDB.RouterInfo;

public class Tunnel {
    // array list of router info
    private HashMap<Integer, RouterInfo> routers;

    public Tunnel() {
        this.routers = new HashMap<>();
    }

    public Tunnel(HashMap<Integer, RouterInfo> routers) {
        this.routers = routers;
    }

    public void addTunnelObject(Integer tunnelID, RouterInfo router) {
        // Add a TunnelObject to the tunnel
        routers.put(tunnelID, router);
    }

    public Object getTunnelObject(int tunnelObjectID) {
        // Get the TunnelObject from the tunnel
        return routers.get(tunnelObjectID);
    }
}
