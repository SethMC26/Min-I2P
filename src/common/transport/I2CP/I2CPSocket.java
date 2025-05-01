package common.transport.I2CP;

import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class I2CPSocket extends Socket {
    /**
     * Input stream
     */
    private Scanner recv;
    /**
     * Output stream
     */
    private PrintWriter send;

    /**
     * Constructs a new I2CPSocket stream and connects to port {@code port} on named host {@code host}
     * @param host Named host, or null for loopback address
     * @param port the port number
     * @throws IOException throws if there is an I/O error creating, connecting or establishing I/O Streams of Socket
     */
    public I2CPSocket(String host, int port) throws IOException {
        super(host,port);

        this.recv = new Scanner(getInputStream());
        this.send = new PrintWriter(getOutputStream(), true);
    }

    /**
     * Constructs a new I2CPSocket stream which wraps an existing connected Socket Stream {@code sock}
     * @apiNote Useful to wrap a socket given by {@code ServerSocket.accept()};
     * @param sock Connected Socket Stream to wrap
     * @throws IOException throws if there is an I/O error creating, connecting or establishing I/O Streams of Socket
     */
    public I2CPSocket(Socket sock) throws IOException{
        super();

        this.recv = new Scanner(sock.getInputStream());
        this.send = new PrintWriter(sock.getOutputStream(), true);
    }

    /**
     * Send an I2CPMessage along the input stream C<->R
     * @param message I2CPMessage to send
     */
    public void sendMessage(I2CPMessage message) {
        send.println(message.toJSONType().toJSON());
    }

    /**
     * Receives I2CPMessage on this Socket's input stream
     * @return I2CPMessage child class received by Socket
     * @throws InvalidObjectException throws if received string cannot be parsed or is of wrong type
     */
    public I2CPMessage getMessage() throws InvalidObjectException {
        try {
            JSONObject json =  JsonIO.readObject(recv.nextLine());
            I2CPMessage message = new I2CPMessage(json);
            //Logger.getInstance().debug("I2CPSock got message " + message.toJSONType().getFormattedJSON());
            switch(message.getType()) {
                case CREATESESSION -> {
                    return new CreateSession(json);
                }
                case SESSIONSTATUS -> {
                    return new SessionStatus(json);
                }
                case SENDMESSAGE -> {
                    return new SendMessage(json);
                }
                case MESSAGESTATUS -> {
                    return new MessageStatus(json);
                }
                case DESTROYSESSION -> {
                    return new DestroySession(json);
                }
                case DESTLOOKUP -> {
                    return new DestinationLookup(json);
                }
                case DESTREPLY -> {
                    return new DestinationReply(json);
                }
                //lease set messages not implemeneted im not sure if we will need them
                case CREATELEASESET -> {
                    throw new UnsupportedOperationException("Createlease set not implemented");
                }
                case REQUESTLEASESET -> {
                    throw new UnsupportedOperationException("Request Leaseset not implemented");
                }

                default -> {
                    throw new InvalidObjectException("Bad type: " + message.getType());
                }
            }
        } catch (NullPointerException e) {
            throw new InvalidObjectException("Could not parse JSON: " + e.getMessage());
        } catch (ClassCastException e) {
            throw new InvalidObjectException("Could not cast to JSONObject: " + e.getMessage());
        }
    }

    public boolean hasMessage() {
        return this.recv.hasNextLine();
    }
}
