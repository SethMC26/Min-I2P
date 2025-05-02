package common.I2P.tunnels;

import java.util.ArrayList;
import java.util.HashMap;

import org.bouncycastle.jcajce.provider.asymmetric.mldsa.MLDSAKeyFactorySpi.Hash;

import common.I2P.NetworkDB.RouterInfo;

public class Tunnel {
    // array list of router info
    private ArrayList<RouterInfo> routers;

    public Tunnel() {
        this.routers = new ArrayList<>();
    }

    public Tunnel(ArrayList<RouterInfo> routers) {
        this.routers = routers;
    }

    public void addTunnelObject(RouterInfo router) {
        // Add a TunnelObject to the tunnel
        routers.add(router);
    }
}
