package testPeers;

import common.I2P.router.Router;
import common.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.Security;

public class p4 {
    static int routerPort = 10004;
    static int servicePort = 20004;
    public static void main(String[] args) throws IOException {
        Security.addProvider(new BouncyCastleProvider());
        Logger log = Logger.getInstance();
        log.setMinLevel(Logger.Level.DEBUG);
        Router router = new Router(routerPort, 8080, false);
    }
}

