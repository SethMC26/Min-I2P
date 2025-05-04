package testPeers;

import common.I2P.router.Router;
import common.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class p2 {
    static int routerPort = 10002;
    static int servicePort = 20002;
    static InetSocketAddress bootstrapPeer = new InetSocketAddress("127.0.0.1", 8080);

    public static void main(String[] args) throws IOException {
        int numberOfRouters = 5; // Specify the number of routers to create
        Logger log = Logger.getInstance();
        log.setMinLevel(Logger.Level.DEBUG);
        Thread router = new Thread(new Router(InetAddress.getLoopbackAddress(),routerPort, servicePort, bootstrapPeer));
        router.start();
    }
}
