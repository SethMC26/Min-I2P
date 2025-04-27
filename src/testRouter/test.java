package testRouter;

import java.io.IOException;

import common.I2P.router.Router;

public class test {
    public static void main(String[] args) {
        try {
            Router router1 = new Router(7000, 8082);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            Router router2 = new Router(7001, 8082);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
