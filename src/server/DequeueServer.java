package server;

import common.transport.I2CP.I2CPSocket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class DequeueServer implements Runnable {

    // -------- Private Variables -------- //
    private final int SESSION_ID;
    private final ConcurrentHashMap<String, ClientState> MAP;
    private final LinkedBlockingQueue<ClientState> QUEUE;
    private final I2CPSocket SOCKET;

    @Override
    public void run() {

    }
}
