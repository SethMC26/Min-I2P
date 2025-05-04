package client;

import common.MessageSocket;
import common.message.ByteMessage;
import common.message.Message;
import common.message.Response;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class EnqueueThread implements Runnable {

    private final LinkedBlockingQueue<byte[]> QUEUE;
    private final MessageSocket SOCKET;

    /**
     * This constructor initializes the enqueue thread which makes sure that the queue is not empty.
     *
     * @param queue - ConcurrentLinkedQueue of byte arrays
     * @param socket - MessageSocket object to communicate with the server
     */
    public EnqueueThread(LinkedBlockingQueue<byte[]> queue, MessageSocket socket) {
        this.QUEUE = queue;
        this.SOCKET = socket;
    }

    /**
     * This method runs the thread and receives messages from the server.
     */
    @Override
    public void run() {
        // Receive the first message from the server
        Message recvMsg = SOCKET.getMessage();

        // Check if the message is a status message
        if (recvMsg.getType().equals("Status")) {
            Response response = (Response) recvMsg;
            System.err.println("Error: " + response.getPayload());
            SOCKET.close();
            System.exit(1);
        }

        // Check if the message is a byte message
        while (recvMsg.getType().equals("Byte")) {

            byte[] combinedAudio = new byte[0];
            for (int i = 0; i < 64; i++) {
                // Combine the byte arrays into one
                if (!recvMsg.getType().equals("Byte")) {
                    System.err.println("Error: Received message is not a byte message");
                    SOCKET.close();
                    System.exit(1);
                }

                ByteMessage byteMessage = (ByteMessage) recvMsg;
                byte[] audio = byteMessage.getData();
                combinedAudio = combineByteArrays(combinedAudio, audio);

                recvMsg = SOCKET.getMessage();

                if (recvMsg.getType().equals("End")) {
                   break;
                }
            }
            // Add the audio to the queue
            QUEUE.add(combinedAudio);
            // System.out.println("EnqueueThread: Added audio of size: " + combinedAudio.length + " bytes number: " + QUEUE.size());
            // Receive the next message from the server

            if (recvMsg.getType().equals("End")) {
                byte[] endAudio = new byte[1];
                endAudio[0] = (byte) 0xff;

                QUEUE.add(endAudio);
            } else {
                recvMsg = SOCKET.getMessage();
            }

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