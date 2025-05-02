package testPeers;

import java.io.IOException;

import common.I2P.router.Router;

public class rt1 {
    public static void main(String[] args) {
        int numberOfRouters = 5; // Specify the number of routers to create

        for (int i = 0; i < numberOfRouters; i++) {
            int port1 = 10001 + i * 2;
            int port2 = 20001;

            Thread routerThread = new Thread(() -> {
                try {
                    Router router = new Router(port1, port2, true);
                    System.out.println("Router started on ports: " + port1 + " and " + port2);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            routerThread.start();
        }
    }
}
