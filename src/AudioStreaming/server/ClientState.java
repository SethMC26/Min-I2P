package AudioStreaming.server;

import common.I2P.IDs.Destination;

public class ClientState {

    // -------- Private Variables -------- //
    private final Destination CLIENTDEST;
    private CommandType commandType;

    private boolean isAuthenticated;
    private String clientName;
    private String clientPassword;
    private int clientOTP;
    private String songname;
    private byte[] songData;
    private int songSize;
    private int byteID;

    /**
     * Creates the AudioStreaming.client state for the AudioStreaming.server to store so it knows the state of the AudioStreaming.client
     *
     * @param clientDest - Destination of the AudioStreaming.client
     * @param commandType - CommandType of the previous command
     */
    public ClientState(Destination clientDest, CommandType commandType) {
        this.CLIENTDEST = clientDest;
        this.commandType = commandType;
    }

    // -------- Getters -------- //

    public Destination getClientDest() {
        return CLIENTDEST;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public String getClientName() {
        return clientName;
    }

    public String getClientPassword() {
        return clientPassword;
    }

    public int getClientOTP() {
        return clientOTP;
    }

    public String getSongname() {
        return songname;
    }

    public int getSongSize() {
        return songSize;
    }

    public byte[] getSongData() {
        return songData;
    }

    public int getByteID() {
        return byteID;
    }

    // -------- Setters -------- //

    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setClientPassword(String clientPassword) {
        this.clientPassword = clientPassword;
    }

    public void setClientOTP(int clientOTP) {
        this.clientOTP = clientOTP;
    }

    public void setSongname(String songname) {
        this.songname = songname;
    }

    public void setSongSize(int songSize) {
        this.songSize = songSize;
    }

    public void setSongData(byte[] songData) {
        this.songData = songData;
    }

    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }

    public void setByteID(int byteID) {
        this.byteID = byteID;
    }

    // -------- Other Methods -------- //

    /**
     * Cleans the AudioStreaming.client state so it can be reused
     */
    public void clean() {
        this.clientName = null;
        this.clientPassword = null;
        this.clientOTP = -1;
        this.songname = null;
        this.songData = null;
        this.commandType = null;
        this.songSize = -1;
        this.byteID = -1;
    }

    /**
     * Cleans the song data so it can be reused
     */
    public void cleanSongData() {
        this.songData = null;
        this.commandType = null;
        this.byteID = -1;
    }

}
