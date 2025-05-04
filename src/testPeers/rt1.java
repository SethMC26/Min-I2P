package testPeers;

import common.I2P.router.Router;
import common.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class rt1 {
    static InetSocketAddress bootstrapPeer = new InetSocketAddress("127.0.0.1", 8080);

    public static void main(String[] args) {
        int numberOfRouters = 5; // Specify the number of routers to create
        Logger log = Logger.getInstance();
        log.setMinLevel(Logger.Level.DEBUG);

        for (int i = 0; i < numberOfRouters; i++) {
            int rstPort = 10001 + i;
            int cstPort = 20001 + i;

            Thread routerThread = new Thread(new Router(InetAddress.getLoopbackAddress(), rstPort, cstPort, bootstrapPeer));

            routerThread.start();
        }
    }
}
