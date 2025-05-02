package client;

import common.MessageSocket;
import common.message.Message;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DequeueThread implements Runnable {

    private final ConcurrentLinkedQueue<byte[]> QUEUE;

    public DequeueThread(ConcurrentLinkedQueue<byte[]> queue) {
        this.QUEUE = queue;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        byte[] audio = QUEUE.poll();

        // Check if the audio is null or empty
        while (audio == null || audio.length == 0) {
            System.err.println("Error: Audio is null in DequeueThread");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            audio = QUEUE.poll();
        }

        while (audio != null || audio.length != 0) {
            // Process the audio data

            // Process the audio data
            AudioFormat audioFormat = new AudioFormat(44100, 16, 2, true, false);

            try {
                AudioInputStream audioStream = new AudioInputStream(new ByteArrayInputStream(audio), audioFormat, audio.length / audioFormat.getFrameSize());
                DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);

                Clip clip = (Clip) AudioSystem.getLine(info);

                clip.open(audioStream);
                clip.start();

                // Wait for the clip to finish playing
                while (clip.isRunning()) {
                    Thread.sleep(100);
                }
                clip.close();

            } catch (LineUnavailableException | IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            audio = QUEUE.poll();
        }


    }
}
