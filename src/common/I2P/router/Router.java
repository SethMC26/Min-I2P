package common.I2P.router;

import common.I2P.I2NP.DatabaseStore;
import common.I2P.I2NP.I2NPHeader;
import common.I2P.I2NP.I2NPMessage;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.tunnels.Tunnel;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class represents Core Router module of I2P
 */
public class Router implements Runnable{
    /**
     * Map of current inbound Tunnels, key is Tunnel ID and Value is appropriate Tunnel
     */
    ConcurrentHashMap<Integer, Tunnel> inboundTunnels = new ConcurrentHashMap<>();
    /**
     * Map of current outbound Tunnels, key is Tunnel ID and Value is appropriate Tunnel
     */
    ConcurrentHashMap<Integer, Tunnel> outboundTunnels = new ConcurrentHashMap<>();
    /**
     * Socket is for connecting to client using I2CP protocol
     */
    ServerSocket clientSock;

    /**
     * Create router from specified config file
     * @param configFile JSON File to use for configuration
     */
    Router(File configFile) throws IOException{
        //todo add config parsing
        setUp();
    }

    /**
     * Create router using a default config file
     */
    Router() throws IOException{
        //todo add config parsing
        setUp();
    }

    private void setUp() throws IOException {
        //todo set proper date from config
        //todo add bootstrap peer
        //setup server socket to communicate with client(application layer)
        clientSock = new ServerSocket(7000); //hard coded for now we will fix later

        //todo connect to bootstrap peer to get initial networkdb and tunnels
    }

    /**
     * Route incoming I2NP messages to appropriate tunnel
     * @param tunnelID Integer ID of tunnel to route message to
     * @param message I2NPMessage to route
     */
    public void routeMessage(Integer tunnelID, I2NPMessage message) throws IOException {
        //todo check if tunnel is inbound or outbound and route accordingly
        Tunnel tunnel = inboundTunnels.get(tunnelID);

        if (tunnel != null) {
           tunnel = outboundTunnels.get(tunnelID);
        }

        if (tunnel != null) {
            tunnel.handleMessage(message);
        } else {
            throw new IOException("Tunnel not found for ID: " + tunnelID);
        }
    }

    @Override
    public void run() {
        //todo here we will manage tunnels and also client messages

        //example I2NP message for Sam :3
        DatabaseStore storemsg = new DatabaseStore((byte[]) null, (RouterInfo) null);
        //header effectively will wrap an I2NPMessage
        I2NPHeader I2NPmsg = new I2NPHeader(I2NPHeader.TYPE.DATABASESTORE, 1111, System.currentTimeMillis(), storemsg);
    }
}
