package testPeers;

import common.I2P.router.Router;
import common.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.Security;

public class p5 {
    static int routerPort = 10005;
    static int servicePort = 20005;
    static InetSocketAddress bootstrapPeer = new InetSocketAddress("127.0.0.1", 8080);

    public static void main(String[] args) throws IOException {
        Security.addProvider(new BouncyCastleProvider());
        Logger log = Logger.getInstance();
        log.setMinLevel(Logger.Level.DEBUG);

        Thread router = new Thread(new Router(InetAddress.getLoopbackAddress(), routerPort, servicePort, bootstrapPeer));
        router.start();
    }
}

