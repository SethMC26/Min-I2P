package test;

import common.I2P.I2NP.I2NPHeader;
import common.I2P.I2NP.TunnelBuild;
import common.I2P.NetworkDB.NetDB;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.router.Router;
import common.I2P.router.RouterServiceThread;
import common.I2P.IDs.RouterID;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

public class RouterTest {

    public static void main(String[] args) throws Exception {
        // Create a mock Router instance
        Router mockRouter = new Router(7000, 8080) {
            @Override
            public List<RouterInfo> queryNetDBForRouters(int k) {
                // Return a mocked list of RouterInfo objects
                List<RouterInfo> mockPeers = new ArrayList<>();
                for (int i = 0; i < k; i++) {
                    try {
                        mockPeers.add(createMockRouterInfo(i));
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
                return mockPeers;
            }
        };

        // Create a mock NetDB
        NetDB mockNetDB = new NetDB(mockRouter.routerInfo);

        // Create a TunnelBuild object
        int numHops = 3; // Number of hops in the tunnel
        TunnelBuild tunnelBuild = mockRouter.createTunnelBuild(numHops);

        // Create a mock I2NPHeader containing the TunnelBuild
        I2NPHeader mockHeader = new I2NPHeader(
                I2NPHeader.TYPE.TUNNELBUILD,
                new Random().nextInt(), // Unique message ID
                System.currentTimeMillis() + 1000, // Expiration time
                tunnelBuild);

        // Create a mock RouterServiceThread
        RouterServiceThread mockServiceThread = new RouterServiceThread(mockNetDB, mockRouter.routerInfo, null,
                mockRouter.tunnelManager) {
            protected void handleTunnelBuildMessage(TunnelBuild tunnelBuild) {
                System.out.println("Handling TunnelBuild message...");
                for (TunnelBuild.Record record : tunnelBuild.getRecords()) {
                    System.out.println(
                            "Forwarding to next hop: " + Base64.getEncoder().encodeToString(record.getNextIdent()));
                }
            }

            @Override
            public void run() {
                super.run();
                // Explicitly call handleTunnelBuildMessage with the TunnelBuild from the mockHeader
                handleTunnelBuildMessage((TunnelBuild) mockHeader.getMessage());
            }
        };

        // Set the receivedMessage and run the service thread
        mockServiceThread.setReceivedMessage(mockHeader); // Assuming setReceivedMessage is a method to set the message
        mockServiceThread.run();
    }

    // Helper method to create a mock RouterInfo object
    private static RouterInfo createMockRouterInfo(int index) throws NoSuchAlgorithmException {
        // Generate mock keys for the RouterID
        KeyPair elgamalKeyPair = KeyPairGenerator.getInstance("ElGamal").generateKeyPair();
        KeyPair edKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        RouterID mockRouterID = new RouterID(elgamalKeyPair.getPublic(), edKeyPair.getPublic());

        // Create a mock RouterInfo object
        return new RouterInfo(mockRouterID, System.currentTimeMillis(), "127.0.0." + (index + 1), 7000 + index,
                edKeyPair.getPrivate());
    }
}