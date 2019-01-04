package ru.gravit.launcher;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;

import java.io.IOException;

public class ClientPermissions {
    public static final ClientPermissions DEFAULT = new ClientPermissions();
    @LauncherAPI
    public boolean canAdmin;
    @LauncherAPI
    public boolean canServer;
    public ClientPermissions(HInput input) throws IOException {
        canAdmin = input.readBoolean();
        canServer = input.readBoolean();
    }
    public ClientPermissions() {
        canAdmin = false;
        canServer = false;
    }

    public ClientPermissions(long data) {
        canAdmin = (data & (1)) != 0;
        canServer = (data & (1 << 1)) != 0;
    }

    public static ClientPermissions getSuperuserAccount() {
        ClientPermissions perm = new ClientPermissions();
        perm.canServer = true;
        perm.canAdmin = true;
        return perm;
    }
    public void write(HOutput output) throws IOException {
        output.writeBoolean(canAdmin);
        output.writeBoolean(canServer);
    }
}
