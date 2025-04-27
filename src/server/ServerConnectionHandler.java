package server;

import java.net.Socket;

public class ServerConnectionHandler implements Runnable {

    // ---------- Private Variables ---------- //
    private AudioDatabase audioDatabase;
    private UsersDatabase usersDatabase;
    private Socket socket;

    ServerConnectionHandler(Socket socket, AudioDatabase audioDatabase, UsersDatabase usersDatabase) {
        this.audioDatabase = audioDatabase;
        this.usersDatabase = usersDatabase;
        this.socket = socket;
    }

    @Override
    public void run() {

    }
}
