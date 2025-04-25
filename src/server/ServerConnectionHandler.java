package server;

public class ServerConnectionHandler implements Runnable {

    // ---------- Private Variables ---------- //
    private AudioDatabase audioDatabase;
    private UsersDatabase usersDatabase;

    ServerConnectionHandler(AudioDatabase audioDatabase, UsersDatabase usersDatabase) {
        this.audioDatabase = audioDatabase;
        this.usersDatabase = usersDatabase;
    }

    @Override
    public void run() {

    }
}
