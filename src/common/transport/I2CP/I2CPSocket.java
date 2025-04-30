package common.transport.I2CP;

import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class I2CPSocket extends Socket {
    private Scanner recv; //input stream
    private PrintWriter send; //output stream


    public I2CPSocket(String host, int port) throws IOException {
        super(host,port);

        this.recv = new Scanner(getInputStream());
        this.send = new PrintWriter(getOutputStream(), true);
    }

    public I2CPSocket() throws IOException {
        super();
    }
    /**
     * Constructs a new JSONSocket stream which wraps an existing connected Socket Stream {@code sock}
     * @apiNote Useful to wrap a socket given by {@code ServerSocket.accept()};
     * @param sock Connected Socket Stream to wrap
     * @throws IOException throws if there is an I/O error creating, connecting or establishing I/O Streams of Socket
     */
    public I2CPSocket(Socket sock) throws IOException{
        super();

        this.recv = new Scanner(sock.getInputStream());
        this.send = new PrintWriter(sock.getOutputStream(), true);
    }


    public void sendMessage(I2CPMessage message) {
        send.println(message.toJSONType().toJSON());
    }


    /**
     * Receives JSONObject on this Socket's input stream
     * @return JSONObject received by Socket
     * @throws InvalidObjectException throws if received string cannot be parsed or is of wrong type
     */
    public I2CPMessage getMessage() throws InvalidObjectException {
        try {
            JSONObject json =  JsonIO.readObject(recv.nextLine());
            I2CPMessage message = new I2CPMessage(json);
            switch(message.getType()) {
                case CREATESESSION -> {
                    return new CreateSession(json);
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
}
