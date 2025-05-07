package AudioStreaming.server;

public enum CommandType {

    NONE, // No command

    CREATE, // Create an account in the AudioStreaming.server

    AUTHENTICATE, // Authenticate the user

    ADD, // Add a song to the database

    SENDING, // Sending a song to the AudioStreaming.server

    END, // End the sending of a song

    PLAY, // Play a song from the database

    LIST // List all songs in the database
}
