package common.I2P.router;

import common.I2P.I2NP.DatabaseStore;
import common.I2P.I2NP.I2NPHeader;
import common.I2P.I2NP.I2NPMessage;
import common.I2P.IDs.RouterID;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.tunnels.Tunnel;
import common.I2P.tunnels.TunnelEndpoint;
import common.I2P.tunnels.TunnelGateway;
import common.I2P.tunnels.TunnelManager;
import common.I2P.tunnels.TunnelParticipant;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class represents Core Router module of I2P
 */
public class Router implements Runnable{
    /**
     * TunnelManager is responsible for managing tunnels
     */
    TunnelManager tunnelManager = new TunnelManager();
    
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
     * Build a tunnel using TunnelEndpoint, TunnelGateway, and TunnelParticipant classes
     * We are operating under the assumption that tunnels do not have time outs and are persistent
     * When someone connects to the network, they will call this method to build outbound and inbound tunnels for themselves
     * Reminder: we need some way in the netdb to mark what users the tunnels correspond to
     * - that info should be retrieved from the build request message sent to the router from the client
     */
    public void buildTunnels() {
        //todo build tunnel using TunnelEndpoint, TunnelGateway, and TunnelParticipant classes

        // generate a random tunnel ID
        Integer tunnelID = (int) (Math.random() * Integer.MAX_VALUE); // random tunnel id
        // add check to netdb to see if this tunnel id is already in use

        Tunnel outboundTunnel = new Tunnel();

        // set this router as the tunnel gateway
        TunnelGateway tunnelGateway = new TunnelGateway(null, null, null, null, null, null, null);

        // add the tunnel gateway to the tunnel manager
        outboundTunnel.addTunnelObject(tunnelGateway);

        // randomly select tunnel participants and add them to the tunnel
        List<TunnelParticipant> tunnelParticipants = new ArrayList<>();
        for (int i = 0; i < 5; i++) { // example: 5 participants
            // in reality, you would select random routers from the network database
            TunnelParticipant participant = new TunnelParticipant(null, null, null, null, null, null, null);
            outboundTunnel.addTunnelObject(participant);
        }

        // select a random tunnel endpoint and add it to the tunnel
        // query the network database for a random router
        TunnelEndpoint tunnelEndpoint = new TunnelEndpoint(null, null, null, null, null, null, null);
        
        // add the endpoint to the tunnel
        outboundTunnel.addTunnelObject(tunnelEndpoint);

        // todo: generate build message
        // send build message to all participants and endpoint
        // if recieived a good response, then we can add the tunnel to the tunnel manager

        // add the tunnel to the tunnel manager
        tunnelManager.addOutboundTunnel(tunnelID, outboundTunnel);

    }
    
    /**
     * Route incoming I2NP messages to appropriate tunnel
     * @param tunnelID Integer ID of tunnel to route message to
     * @param message I2NPMessage to route
     */
    public void routeMessage(Integer tunnelID, I2NPMessage message) throws IOException {
        //todo check if tunnel is inbound or outbound and route accordingly
        
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
