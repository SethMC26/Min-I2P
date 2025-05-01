package common.I2P.router;

import common.I2P.NetworkDB.RouterInfo;
import common.Logger;
import common.transport.I2CP.*;
import common.transport.I2NPSocket;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.ServerSocket;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static common.transport.I2CP.I2CPMessageTypes.CREATESESSION;
import static common.transport.I2CP.I2CPMessageTypes.SENDMESSAGE;

public class ClientServiceThread implements Runnable {
    /**
     * Socket for router communications
     */
    private I2NPSocket routerSock;
    /**
     * Router this thread is a apart of
     */
    private RouterInfo router;
    /**
     * Server for client connections
     */
    private ServerSocket server;
    /**
     * Logger
     */
    private Logger log = Logger.getInstance();
    /**
     * Secure random for generating message ids
     */
    private SecureRandom random = new SecureRandom();
    /**
     * Queue to get messages from RouterServiceThread
     */
    private ConcurrentLinkedQueue<I2CPMessage> clientMessage;

    /**
     * Create new thread to service incoming client connects
     * @param router This router we are apart of
     * @param port Port for clients to connect on
     * @param clientMessages Queue of messages from client
     * @throws IOException if could not create ServerSocket or I2NPSocket
     */
    public ClientServiceThread(RouterInfo router, int port, ConcurrentLinkedQueue<I2CPMessage> clientMessages) throws IOException {
        this.router = router;
        this.server = new ServerSocket(port);
        this.routerSock = new I2NPSocket();
        this.clientMessage = clientMessages;
    }

    @Override
    public void run() {
        try {
            ExecutorService threadpool = Executors.newFixedThreadPool(1); //we only have one client application so we dont need multiple threads
            //todo ask sean if it is helpful for the server to have multiple connections
            //wait for connections
            while(true) {
                I2CPSocket clientSock = new I2CPSocket(server.accept());
                threadpool.execute(new ClientConnectionHandler(clientSock));
            }

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

    private class ClientConnectionHandler implements Runnable {
        /**
         * Socket for client communication for this session
         */
        private I2CPSocket clientSock; //socket to communicate with client
        /**
         * ID of session
         */
        private int sessionID;
        ClientConnectionHandler(I2CPSocket clientSock) {
            this.clientSock = clientSock;
        }
        @Override
        public void run() {
            try {
                log.debug("CST: Got connection from client");
                I2CPMessage recvMsg = clientSock.getMessage();

                if (isTypeBad(recvMsg, CREATESESSION)){ //bad type we(router) will refuse connection
                    clientSock.sendMessage(new SessionStatus(recvMsg.getSessionID(), SessionStatus.Status.REFUSED));
                    clientSock.close();
                    return;
                }

                //generate new id for session
                sessionID = random.nextInt();
                //accept session
                clientSock.sendMessage(new SessionStatus(sessionID, SessionStatus.Status.CREATED));

                //todo setup necessary information in router, hashmap with sessionIDs/destinations/messageQueues?

                while(true) { //might be a better way to do this that avoids busy waiting
                    //wait until a new message on socket or a new message has arrived from router
                    if (!clientSock.hasMessage() && clientMessage.isEmpty())
                        continue;

                    //deal with client messages
                    if (clientSock.hasMessage()) {
                        I2CPMessage message = clientSock.getMessage();
                        log.debug("Handling message type " + message.getType());
                        switch(message.getType()) {
                            case SENDMESSAGE -> {
                                SendMessage send = (SendMessage) message;

                                clientSock.sendMessage(send); //for testing just echo message back
                                //todo set up session information in router
                                //routerSock.send(router, new I2NPHeader()) //send tunnel data to router to send on outbound tunnel
                            }
                            case DESTROYSESSION -> {
                                clientSock.close();
                                //todo handle any necessary session destroying in router
                                return; //close thread
                            }
                            default -> {
                                log.error("Bad type received from client only send and destroy allowed " + message.getType());
                                MessageStatus status = new MessageStatus(sessionID, 0, new byte[4], MessageStatus.Status.BADMESSAGE);
                                clientSock.sendMessage(status);
                            }
                        }
                    }

                    if (!clientMessage.isEmpty()) {
                        I2CPMessage message = clientMessage.remove();
                        //todo handle getting message from router and giving it back to the client
                        if (message.getType() != SENDMESSAGE) {
                            log.warn("Bad message from router");
                            continue;
                        }

                        clientSock.sendMessage(message);
                    }
                }
            } catch (InvalidObjectException e) {
                log.error("CST-CCH: Bad message ", e);
                try {
                    clientSock.sendMessage(new SessionStatus(sessionID, SessionStatus.Status.DESTROYED));
                    clientSock.close();
                } catch (IOException ex) {
                    log.warn("Could not close client sock", e);
                }
            } catch (IOException e) {
                log.error("CST-CCH: IOException occured", e);
                try {
                    clientSock.sendMessage(new SessionStatus(sessionID, SessionStatus.Status.DESTROYED));
                    clientSock.close();
                } catch (IOException ex) {
                    log.warn("Could not close client sock", e);
                }
            }
        }
    }
}
