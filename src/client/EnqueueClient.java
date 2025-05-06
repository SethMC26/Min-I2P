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
            System.out.println("EnqueueClient started.");

            I2CPMessage recvMsg = SOCKET.getMessage();

            if (!(recvMsg instanceof PayloadMessage)) {
                System.err.println("Error: expected PayloadMessage, got " + recvMsg.getType());
                return;
            }

            PayloadMessage payloadMessage = (PayloadMessage) recvMsg;
            Message recvMessage = new Message(payloadMessage.getPayload());

            while (recvMessage.getType().equals("Byte")) {

                System.out.println("Received message type: " + recvMessage.getType());

                byte[] combinedAudio = new byte[0];
                for (int i = 0; i < 64; i++) {

                    if (!recvMessage.getType().equals("Byte")) {
                        System.err.println("Error: expected ByteMessage, got " + recvMessage.getType());
                        break;
                    }

                    ByteMessage byteMessage = new ByteMessage(payloadMessage.getPayload());
                    byte[] audio = byteMessage.getData();

                    combinedAudio = combineByteArrays(combinedAudio, audio);

                    recvMsg = SOCKET.getMessage();

                    if (!(recvMsg instanceof PayloadMessage)) {
                        System.err.println("Error: expected PayloadMessage, got " + recvMsg.getType());
                        return;
                    }

                    payloadMessage = (PayloadMessage) recvMsg;
                    recvMessage = new Message(payloadMessage.getPayload());

                    if (recvMessage.getType().equals("End")) {
                        break;
                    }

                }

                QUEUE.add(combinedAudio);
                System.out.println("Enqueued queue size: " + QUEUE.size());

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