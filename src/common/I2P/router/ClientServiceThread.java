package common.I2P.router;

import common.I2P.NetworkDB.RouterInfo;
import common.Logger;
import common.transport.I2CP.I2CPMessage;
import common.transport.I2CP.I2CPMessageTypes;
import common.transport.I2CP.I2CPSocket;
import common.transport.I2CP.SessionStatus;
import common.transport.I2NPSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.HashMap;

import static common.transport.I2CP.I2CPMessageTypes.CREATESESSION;

public class ClientServiceThread implements Runnable {
    private I2NPSocket routerSock;
    private RouterInfo router;
    private Socket clientSock;
    private ServerSocket server;
    private Logger log = Logger.getInstance();
    private SecureRandom random = new SecureRandom();
    private HashMap<Integer, I2CPSocket> sessionMap = new HashMap();

    public ClientServiceThread(RouterInfo router, int port) throws IOException {
        this.server = new ServerSocket(port);
        this.routerSock = new I2NPSocket();
    }

    @Override
    public void run() {
        try {
            I2CPSocket clientSock = new I2CPSocket(server.accept()); //we only have one client application so we dont need multiple threads
            log.debug("CST: Got connection from " + clientSock.getPort());
            I2CPMessage recvMsg = clientSock.getMessage();

            if (isTypeBad(recvMsg, CREATESESSION)){ //bad type we(router) will refuse connection
                clientSock.sendMessage(new SessionStatus(recvMsg.getSessionID(), SessionStatus.Status.REFUSED));
                return;
            }

            //generate new id for session
            int sessionID = random.nextInt();
            while(sessionMap.containsKey(sessionID)) //try to generate a sessionID not in router
                sessionID = random.nextInt();

            clientSock.sendMessage(new SessionStatus(sessionID, SessionStatus.Status.CREATED));
            I2CPMessage recvMSg = clientSock.getMessage();

            clientSock.sendMessage(recvMSg);
        } catch (IOException e) {
            log.error("CST: IO exception" ,e);
        }
    }

    private boolean isTypeBad(I2CPMessage toCheck, I2CPMessageTypes expectedType) {
        if (toCheck.getType() != expectedType) {
            log.warn("Bad type of message, got " + toCheck.getType() + " but expected " + expectedType);
            log.debug("Message with bad type " + toCheck.toJSONType().getFormattedJSON());
            return true;
        }
        return false;
    }
}
