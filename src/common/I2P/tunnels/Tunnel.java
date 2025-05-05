package common.I2P.tunnels;

import java.util.ArrayList;
import java.util.HashMap;

import org.bouncycastle.jcajce.provider.asymmetric.mldsa.MLDSAKeyFactorySpi.Hash;

import common.I2P.NetworkDB.RouterInfo;

public class Tunnel {
    // array list of router info
    private ArrayList<TunnelItem> routers;

    public Tunnel() {
        this.routers = new ArrayList<>();
    }

    public Tunnel(ArrayList<TunnelItem> routers) {
        this.routers = routers;
    }

    public void addTunnelObject(Integer tunnelID, RouterInfo router) {
        // Add a TunnelObject to the tunnel
        TunnelItem tunnelItem = new TunnelItem(tunnelID, router);
        routers.add(tunnelItem);
    }

    public RouterInfo getTunnelObject(int tunnelObjectID) {
        // Get the router info for the given tunnel object ID
        for (TunnelItem item : routers) {
            if (item.getTunnelID() == tunnelObjectID) {
                return item.getRouterInfo();
            }
        }
        return null; // not found
    }

    public int getGatewayTunnelID() {
        // Get the first tunnel ID in the tunnel
        if (routers.size() > 0) {
            return routers.get(0).getTunnelID();
        }
        return -1; // no gateway found
    }

    public RouterInfo getGateway() {
        // Get the first object in the tunnel
        if (routers.size() > 0) {
            return routers.get(0).getRouterInfo();
        }
        return null; // no gateway found
    }

    private class TunnelItem {
        private int tunnelID;
        private RouterInfo routerInfo;

        public TunnelItem(int tunnelID, RouterInfo routerInfo) {
            this.tunnelID = tunnelID;
            this.routerInfo = routerInfo;
        }

        public int getTunnelID() {
            return tunnelID;
        }

        public RouterInfo getRouterInfo() {
            return routerInfo;
        }
    }
}
