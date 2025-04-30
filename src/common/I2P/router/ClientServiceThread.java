package common.I2P.router;

import common.Logger;
import common.transport.I2CP.I2CPMessage;
import common.transport.I2CP.I2CPSocket;
import common.transport.I2NPSocket;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ClientServiceThread implements Runnable {
    private I2NPSocket routerSock;
    private Socket clientSock;
    private ServerSocket server;
    private Logger log = Logger.getInstance();

    public ClientServiceThread(I2NPSocket routerSock, int port) throws IOException {
        this.server = new ServerSocket(port);
    }

    @Override
    public void run() {
        while(true) {
            try {
                I2CPSocket clientSock = (I2CPSocket) server.accept(); //we only have one client application so we dont need multiple threads

                I2CPMessage message = clientSock.getMessage();
            } catch (IOException e) {
                log.error("CST: IO exception");
                throw new RuntimeException(e);
            }

        }
    }
}
