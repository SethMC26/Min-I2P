package testPeers;

import common.I2P.router.Router;
import common.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.Security;

public class b1 {
    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        Logger log = Logger.getInstance();
        log.setMinLevel(Logger.Level.DEBUG);

        //start router
        Thread router = new Thread(new Router(InetAddress.getLoopbackAddress(),8080, 8000, new InetSocketAddress("127.0.0.1", 8080)));
        router.start();
    }
}