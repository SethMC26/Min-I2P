package common.I2P.router;

import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidObjectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class RouterConfig {
    private InetAddress address;
    private int RSTport;
    private int CSTPort;
    private InetSocketAddress bootstrapPeer;

    public RouterConfig(File config) throws FileNotFoundException, InvalidObjectException {
        if (!config.exists())
            throw new FileNotFoundException("Config file does not exist");

        JSONObject json = JsonIO.readObject(config);
        json.checkValidity(new String[] {"bootstrap-peer", "router-addr"});

        //get bootstrap peer info
        JSONObject bootJson = json.getObject("bootstrap-peer");
        bootJson.checkValidity(new String[]{"host", "port"});

        bootstrapPeer = new InetSocketAddress(bootJson.getString("host"), bootJson.getInt("port"));

        //get router info address
        JSONObject routerJSON = json.getObject("router-addr");
        routerJSON.checkValidity(new String[]{"host", "RSTPort", "CSTPort"});

        //try to get address for router
        try {
            address = InetAddress.getByName(routerJSON.getString("host"));
        } catch (UnknownHostException e) {
            throw new InvalidObjectException("Host does not exist " + e);
        }

        //get proper ports
        RSTport = routerJSON.getInt("RSTPort");
        CSTPort = routerJSON.getInt("CSTPort");
    }

    public InetAddress getAddress() {
        return address;
    }

    public InetSocketAddress getBootstrapPeer() {
        return bootstrapPeer;
    }

    public int getCSTPort() {
        return CSTPort;
    }

    public int getRSTport() {
        return RSTport;
    }
}
