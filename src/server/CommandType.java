package server;

public enum CommandType {

    CREATE, // Create an account in the server

    AUTHENTICATE, // Authenticate the user

    ADD, // Add a song to the database

    SENDING, // Sending a song to the server

    END, // End the sending of a song

    PLAY, // Play a song from the database

    LIST // List all songs in the database
}
