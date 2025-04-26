package common.I2P.tunnels;

import java.util.ArrayList;
import java.util.HashMap;

import org.bouncycastle.jcajce.provider.asymmetric.mldsa.MLDSAKeyFactorySpi.Hash;

public class Tunnel {
    // array list of tunnel gateway, participant, and endpoint objects
    private ArrayList<TunnelObject> tunnelObjects = new ArrayList<>();

    public Tunnel() {
        // Constructor for Tunnel class
    }

    public void addTunnelObject(TunnelObject tunnelObject) {
        // Add a TunnelObject to the tunnel
        tunnelObjects.add(tunnelObject);
    }
}
