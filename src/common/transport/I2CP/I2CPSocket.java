package common.transport.I2CP;

import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class I2CPSocket extends Socket {
    /**
     * Input stream
     */
    private BufferedInputStream recv;
    /**
     * Output stream
     */
    private BufferedOutputStream send;

    /**
     * Constructs a new I2CPSocket stream and connects to port {@code port} on named host {@code host}
     * @param host Named host, or null for loopback address
     * @param port the port number
     * @throws IOException throws if there is an I/O error creating, connecting or establishing I/O Streams of Socket
     */
    public I2CPSocket(String host, int port) throws IOException {
        super(host,port);

        this.recv = new BufferedInputStream(getInputStream());
        this.send = new BufferedOutputStream(getOutputStream());
    }

    /**
     * Constructs a new I2CPSocket stream which wraps an existing connected Socket Stream {@code sock}
     * @apiNote Useful to wrap a socket given by {@code ServerSocket.accept()};
     * @param sock Connected Socket Stream to wrap
     * @throws IOException throws if there is an I/O error creating, connecting or establishing I/O Streams of Socket
     */
    public I2CPSocket(Socket sock) throws IOException{
        super();
        this.recv = new BufferedInputStream(sock.getInputStream());
        this.send = new BufferedOutputStream(sock.getOutputStream());
    }

    /**
     * Send an I2CPMessage along the input stream C<->R
     * @param message I2CPMessage to send
     */
    public void sendMessage(I2CPMessage message) throws IOException {
        byte[] jsonbytes = message.serialize().getBytes(StandardCharsets.UTF_8);
        send.write(ByteBuffer.allocate(Integer.BYTES).putInt(jsonbytes.length).array());
        send.write(jsonbytes);
        send.flush();
    }

    /**
     * Receives I2CPMessage on this Socket's input stream
     * @return I2CPMessage child class received by Socket
     * @throws InvalidObjectException throws if received string cannot be parsed or is of wrong type
     */
    public I2CPMessage getMessage() throws InvalidObjectException {
        try {
            int len = ByteBuffer.wrap(recv.readNBytes(4)).getInt();

            byte[] buf = recv.readNBytes(len);

            JSONObject json = JsonIO.readObject(new String(buf, StandardCharsets.UTF_8));

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
                case PAYLOADMESSAGE -> {
                    return new PayloadMessage(json);
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
                case CREATELEASESET -> {
                    return new CreateLeaseSet(json);
                }
                case REQUESTLEASESET -> {
                    return new RequestLeaseSet(json);
                }

                default -> {
                    throw new InvalidObjectException("Bad type: " + message.getType());
                }
            }
        } catch (NullPointerException e) {
            throw new InvalidObjectException("Could not parse JSON: " + e.getMessage());
        } catch (ClassCastException e) {
            throw new InvalidObjectException("Could not cast to JSONObject: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasMessage() throws IOException {
        if (recv.available() < 4)
            return false;

        //below from chatGPT to see if we have enough bytes to read
        recv.mark(4);
        byte[] hdr = recv.readNBytes(4);              // always gives 4 because we checked available()
        int len    = ByteBuffer.wrap(hdr).getInt();
        boolean complete = recv.available() >= len;   // is the whole body here?
        recv.reset();                                 // rewind to original position
        return complete;
    }
}
