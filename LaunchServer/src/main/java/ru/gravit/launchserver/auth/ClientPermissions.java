package ru.gravit.launchserver.auth;

public class ClientPermissions {
    public static final ClientPermissions DEFAULT = new ClientPermissions();
    public boolean canAdmin;
    public boolean canServer;

    public ClientPermissions() {
        canAdmin = false;
        canServer = false;
    }
    public ClientPermissions(long data) {
        canAdmin =  (data & (1 << 0)) != 0;
        canServer = (data & (1 << 1)) != 0;
    }
}
