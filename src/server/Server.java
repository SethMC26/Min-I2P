package server;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Server {

    public static void main(String[] args) {
        BouncyCastleProvider c = new BouncyCastleProvider();
        System.out.println("hello World");
    }
}
