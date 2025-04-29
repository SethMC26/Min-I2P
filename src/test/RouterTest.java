package test;

import common.I2P.I2NP.TunnelBuild;
import common.I2P.NetworkDB.RouterInfo;
import common.I2P.router.Router;
import common.I2P.IDs.RouterID;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouterTest {

    public static void main(String[] args) throws Exception {
        // Step 1: Create a mock Router instance
        Router mockRouter = new Router(7000, 8080) {
            @Override
            public List<RouterInfo> queryNetDBForRouters(int k) {
                // Return a mocked list of RouterInfo objects
                List<RouterInfo> mockPeers = new ArrayList<>();
                for (int i = 0; i < k; i++) {
                    try {
                        mockPeers.add(createMockRouterInfo(i));
                    } catch (NoSuchAlgorithmException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                return mockPeers;
            }
        };

        // Step 2: Call the createTunnelBuild method
        int numHops = 3; // Number of hops in the tunnel
        TunnelBuild tunnelBuild = mockRouter.createTunnelBuild(numHops);

        // Step 3: Print the output for verification
        System.out.println("TunnelBuild created successfully:");
        System.out.println(tunnelBuild.toJSONType().getFormattedJSON());
    }

    // Helper method to create a mock RouterInfo object
    private static RouterInfo createMockRouterInfo(int index) throws NoSuchAlgorithmException {
        // Generate mock keys for the RouterID
        KeyPair elgamalKeyPair = KeyPairGenerator.getInstance("ElGamal").generateKeyPair();
        KeyPair edKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        RouterID mockRouterID = new RouterID(elgamalKeyPair.getPublic(), edKeyPair.getPublic());

        // Create a mock RouterInfo object
        return new RouterInfo(mockRouterID, System.currentTimeMillis(), "127.0.0." + (index + 1), 7000 + index, edKeyPair.getPrivate());
    }
}