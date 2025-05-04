package testPeers;

import common.I2P.router.Router;
import common.Logger;

public class rt1 {
    public static void main(String[] args) {
        int numberOfRouters = 5; // Specify the number of routers to create
        Logger log = Logger.getInstance();
        log.setMinLevel(Logger.Level.DEBUG);

        for (int i = 0; i < numberOfRouters; i++) {
            int port1 = 10001 + i * 2;
            int port2 = 8080;

            Thread routerThread = new Thread(() -> {
                System.out.println("Router started on ports: " + port1 + " and " + port2);
                Router router = new Router(port1, port2);
            });

            routerThread.start();
        }
    }
}
