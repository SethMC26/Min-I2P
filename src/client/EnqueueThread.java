package client;

import common.MessageSocket;
import common.message.ByteMessage;
import common.message.Message;
import common.message.Response;

import java.util.concurrent.ConcurrentLinkedQueue;

public class EnqueueThread implements Runnable {

    private final ConcurrentLinkedQueue<byte[]> QUEUE;
    private final MessageSocket SOCKET;

    /**
     * This constructor initializes the enqueue thread which makes sure that the queue is not empty.
     *
     * @param queue - ConcurrentLinkedQueue of byte arrays
     * @param socket - MessageSocket object to communicate with the server
     */
    public EnqueueThread(ConcurrentLinkedQueue<byte[]> queue, MessageSocket socket) {
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

            ByteMessage message = (ByteMessage) recvMsg;

            byte[] audio = message.getData();

            // Check if the audio is null or empty
            if (audio == null || audio.length == 0) {
                System.err.println("Error: Audio is null in EnqueueThread");
                Response response = new Response("Status", false, "Audio is null");
                SOCKET.sendMessage(response);
                SOCKET.close();
                System.exit(1);
            }

            // Adds the audio to the queue
            QUEUE.add(audio);

            // Gets the next message from the server
            recvMsg = SOCKET.getMessage();

        }
    }

}