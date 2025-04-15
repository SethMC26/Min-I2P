package client;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Client {

    public static void main(String[] args) {
        BouncyCastleProvider c = new BouncyCastleProvider();
        System.out.println("Hello World");
    }
}
