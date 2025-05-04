package testPeers;

import common.I2P.router.Router;
import common.Logger;

import java.io.IOException;

public class p3 {
    static int routerPort = 10003;
    static int servicePort = 20003;
    public static void main(String[] args) throws IOException {
        int numberOfRouters = 5; // Specify the number of routers to create
        Logger log = Logger.getInstance();
        log.setMinLevel(Logger.Level.DEBUG);
        Router router = new Router(routerPort, 8080);
    }
}

