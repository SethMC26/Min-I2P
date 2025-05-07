package AudioStreaming.server;

import AudioStreaming.message.ByteMessage;
import AudioStreaming.message.Message;
import common.transport.I2CP.I2CPSocket;
import common.transport.I2CP.SendMessage;
import org.bouncycastle.util.encoders.Base64;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SendPlayedSong implements Runnable {

    private final ClientState CLIENT_STATE;
    private final String AUDIO_DATA;
    private final I2CPSocket SOCKET;
    private final int SESSION_ID; // Assuming session ID is 0 for simplicity

    public SendPlayedSong(int sessionID ,I2CPSocket socket, ClientState clientState, String audioData) {
        this.SESSION_ID = sessionID;
        this.SOCKET = socket;
        this.CLIENT_STATE = clientState;
        this.AUDIO_DATA = audioData;
    }

    @Override
    public void run() {

        try (BufferedReader br = new BufferedReader(new FileReader(AUDIO_DATA))) {
            String line;
            int i = 0;
            byte[] audioBytes;
            while ((line = br.readLine()) != null) {
                // Assuming the audio data is in Base64 format
                audioBytes = Base64.decode(line);

                if (audioBytes != null) {
                    ByteMessage byteMessage = new ByteMessage("Byte", "", audioBytes, i);
                    SendMessage sendMessage = new SendMessage(SESSION_ID, CLIENT_STATE.getClientDest(), audioBytes, byteMessage.toJSONType());

                    // Send the audio data
                    SOCKET.sendMessage(sendMessage);
                    i++;

                    // Sleep for a short duration to simulate real-time playback
                    Thread.sleep(5); // Adjust the sleep time as needed
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading audio file: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        // Send a termination message to indicate the end of the audio stream
        Message endMessage = new Message("End", "");
        SendMessage endPlay = new SendMessage(SESSION_ID, CLIENT_STATE.getClientDest(), new byte[4], endMessage.toJSONType());
        try {
            SOCKET.sendMessage(endPlay);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
