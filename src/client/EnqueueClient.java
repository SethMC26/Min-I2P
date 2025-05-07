package client;

import common.message.ByteMessage;
import common.message.Message;
import common.transport.I2CP.I2CPMessage;
import common.transport.I2CP.I2CPSocket;
import common.transport.I2CP.PayloadMessage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class EnqueueClient implements Runnable {

    private final LinkedBlockingQueue<byte[]> QUEUE;
    private final I2CPSocket SOCKET;

    /**
     * This constructor initializes the enqueue thread which makes sure that the queue is not empty.
     *
     * @param queue - ConcurrentLinkedQueue of byte arrays
     * @param socket - MessageSocket object to communicate with the server
     */
    public EnqueueClient(LinkedBlockingQueue<byte[]> queue, I2CPSocket socket) {
        this.QUEUE = queue;
        this.SOCKET = socket;
    }

    /**
     * This method runs the thread and receives messages from the server.
     */
    @Override
    public void run() {
        try {

            // Wait for the first message from the server
            I2CPMessage recvMsg = SOCKET.getMessage();

            // Check if the received message is a PayloadMessage
            if (!(recvMsg instanceof PayloadMessage)) {
                System.err.println("Error: expected PayloadMessage, got " + recvMsg.getType());
                return;
            }

            // Extract the payload from the received message
            PayloadMessage payloadMessage = (PayloadMessage) recvMsg;
            Message recvMessage = new Message(payloadMessage.getPayload());

            // Check if the received message is of type "Byte"
            while (recvMessage.getType().equals("Byte")) {

                // Extract the byte data from the received message
                byte[] combinedAudio = new byte[0];
                for (int i = 0; i < 64; i++) {

                    // Wait for the next message from the server
                    if (!recvMessage.getType().equals("Byte")) {
                        System.err.println("Error: expected ByteMessage, got " + recvMessage.getType());
                        break;
                    }

                    // Extract the byte data from the received message
                    ByteMessage byteMessage = new ByteMessage(payloadMessage.getPayload());
                    byte[] audio = byteMessage.getData();

                    // Combine the received audio data with the existing data
                    combinedAudio = combineByteArrays(combinedAudio, audio);

                    // Wait for the next message from the server
                    recvMsg = SOCKET.getMessage();

                    // Check if the received message is a PayloadMessage
                    if (!(recvMsg instanceof PayloadMessage)) {
                        System.err.println("Error: expected PayloadMessage, got " + recvMsg.getType());
                        return;
                    }

                    // Extract the payload from the received message
                    payloadMessage = (PayloadMessage) recvMsg;
                    recvMessage = new Message(payloadMessage.getPayload());

                    // Check if the received message is of type "End"
                    if (recvMessage.getType().equals("End")) {
                        break;
                    }

                }

                // Add the combined audio data to the queue
                QUEUE.add(combinedAudio);

                // Check if the received message is of type "End"
                if (recvMessage.getType().equals("End")) {
                    byte[] endAudio = new byte[1];
                    endAudio[0] = (byte) 0xff;

                    QUEUE.add(endAudio);
                } else {
                    recvMsg = SOCKET.getMessage();

                    if (!(recvMsg instanceof PayloadMessage)) {
                        System.err.println("Error: expected PayloadMessage, got " + recvMsg.getType());
                        return;
                    }

                    payloadMessage = (PayloadMessage) recvMsg;
                    recvMessage = new Message(payloadMessage.getPayload());
                }
            }


        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }

    }

    /**
     * This method combines two byte arrays into one.
     *
     * @param first - byte[] first byte array
     * @param second - byte[] second byte array
     * @return - byte[] combined byte array
     */
    public byte[] combineByteArrays(byte[] first, byte[] second) {
        ByteBuffer buffer = ByteBuffer.allocate(first.length + second.length);
        buffer.put(first);
        buffer.put(second);
        return buffer.array();
    }

}