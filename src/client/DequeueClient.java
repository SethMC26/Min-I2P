package client;

import javax.sound.sampled.*;
import java.util.concurrent.LinkedBlockingQueue;

public class DequeueClient implements Runnable {

    private final LinkedBlockingQueue<byte[]> QUEUE;

    /**
     * This constructor initializes the dequeue thread which makes sure that the queue is not empty.
     *
     * @param queue - LinkedBlockingQueue of byte arrays
     */
    public DequeueClient(LinkedBlockingQueue<byte[]> queue) {
        this.QUEUE = queue;
    }

    @Override
    public void run() {

        while (QUEUE.isEmpty()) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        // Wait for the first message from the queue
        byte[] audio;
        try {
            audio = QUEUE.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Sets the audio format
        AudioFormat audioFormat = new AudioFormat(44100, 16, 2, true, false);

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

            // Starts the audio stream
            line.open(audioFormat);

            line.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    System.out.println("Audio playback completed.");
                    line.close();
                }
            });

            line.start();

            // Write the audio data to the line
            while (!(audio[0] == (byte) 0xff && audio.length == 4)) {
                line.write(audio, 0, audio.length);
                audio = QUEUE.take();
            }

            // Stop the line when the audio is finished
            line.drain();
            line.stop();
            line.close();

        } catch (LineUnavailableException | InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {

        }

    }
}
