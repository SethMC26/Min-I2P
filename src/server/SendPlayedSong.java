package server;

import common.message.ByteMessage;
import common.message.Message;
import common.transport.I2CP.I2CPSocket;
import common.transport.I2CP.SendMessage;

import java.io.IOException;
import java.util.List;

public class SendPlayedSong implements Runnable {

    private final ClientState CLIENT_STATE;
    private final List<byte[]> AUDIO_DATA;
    private final I2CPSocket SOCKET;
    private final int SESSION_ID; // Assuming session ID is 0 for simplicity

    public SendPlayedSong(int sessionID ,I2CPSocket socket, ClientState clientState, List<byte[]> audioData) {
        this.SESSION_ID = sessionID;
        this.SOCKET = socket;
        this.CLIENT_STATE = clientState;
        this.AUDIO_DATA = audioData;
    }

    @Override
    public void run() {
        // Builds all the messages that need to be sent
        for (int i = 0; i < AUDIO_DATA.size(); i++) {
            byte[] data = AUDIO_DATA.get(i);

            // Create a new ByteMessage with the audio data
            ByteMessage msg = new ByteMessage("Byte", "", data, i);
            SendMessage sendPlay = new SendMessage(SESSION_ID, CLIENT_STATE.getClientDest(), new byte[4], msg.toJSONType());

            // Send the message to the client
            try {
                SOCKET.sendMessage(sendPlay);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
