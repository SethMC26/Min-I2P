package common.transport;

import common.I2P.I2NP.I2NPHeader;
import common.I2P.NetworkDB.RouterInfo;
import common.Logger;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class I2NPSocket extends DatagramSocket {
    /**
     * Constructs a datagram socket and binds it to any available port on the local host machine. The socket will be
     * bound to the wildcard address
     * @throws SocketException if the socket could not be opened, or the socket could not be bound
     */
    public I2NPSocket() throws SocketException {
        super();
    }

    /**
     * Creates a I2NP Socket , bound to the specified local address. If the IP address is a wildcard address, or is null,
     * the socket will be bound to the wildcard address.
     * @param port port to bind socket to
     * @param address address to bind socket to
     * @throws SocketException if the socket could not be opened, or the socket could not bind to the specified local port
     */
    public I2NPSocket(int port, InetAddress address) throws SocketException {
        super(port, address);
    }

    /**
     * Send I2NPHeader message
     * @param message I2NP header to send on, sends to remoteSocketAddress connected
     * @apiNote Must call {@code .connect(InetAddress, Port)} before calling this method - in order to send a message
     * we must be connected
     * @throws IOException  if an I/O error occurs.
     * @throws IllegalArgumentException if remoteSocketAddress is null
     */
    public void sendMessage(I2NPHeader message, RouterInfo toSend) throws IOException {
        byte[] messageByte = message.serialize().getBytes(StandardCharsets.UTF_8);
        //if (messageByte.length > MAX_SIZE)
            //throw new RuntimeException("Bytes is over max size! We will need to increase max size");

        InetSocketAddress toSendAddress = new InetSocketAddress(toSend.getHost(), toSend.getPort());
        Logger.getInstance().debug("Sending message " + message.getType() + " to " + toSend.getPort());

        DatagramPacket pkt = new DatagramPacket(messageByte, messageByte.length, toSendAddress);
        send(pkt);
    }

    /**
     * Send a message to a specific router
     * @param message I2NP message to send
     * @param host Host to send message to
     * @param port Port to send message to
     * @throws IOException if IO error occurs
     * @apiNote {@code sendMessage(I2NPHeader message, InetSocketAddress address)} possibly more desirable
     */
    public void sendMessage(I2NPHeader message, String host, int port) throws IOException {
        byte[] messageByte = message.serialize().getBytes(StandardCharsets.UTF_8);

        InetSocketAddress toSendAddress = new InetSocketAddress(host, port);

        DatagramPacket pkt = new DatagramPacket(messageByte, messageByte.length, toSendAddress);
        send(pkt);
    }

    /**
     * Send I2NP message to an InetSocketAddress
     * @param message I2NP message to send
     * @param address Address of message
     * @throws IOException if IO error occurs while sending
     */
    public void sendMessage(I2NPHeader message, InetSocketAddress address) throws IOException {
        byte[] messageByte = message.serialize().getBytes(StandardCharsets.UTF_8);

        DatagramPacket pkt = new DatagramPacket(messageByte, messageByte.length, address);
        send(pkt);
    }

    /**
     * Gets an I2NPMessage over this Datagram socket
     * @return I2NPMessage
     * @throws IOException if an I/O error occurs.
     */
    public I2NPHeader getMessage() throws IOException {
        //very hacky we have big packets ;)
        DatagramPacket pkt = new DatagramPacket(new byte[64_000], 64_000); //size is always an issue for me -seth
        receive(pkt);
        //this is a hacky trick  to only get bytes received in message real protocols add lengths to avoid this issue
        //We are fake mathematician and Engineers anyways whats a little hack among friends
        String json = new String(pkt.getData(), 0, pkt.getLength(), StandardCharsets.UTF_8);
        JSONObject obj = JsonIO.readObject(json);
        return new I2NPHeader(obj);
    }
}
