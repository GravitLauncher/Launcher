package ru.gravit.launcher;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;

import java.io.IOException;

public class ClientPermissions {
    public static final ClientPermissions DEFAULT = new ClientPermissions();
    @LauncherAPI
    public boolean canAdmin = false;
    @LauncherAPI
    public boolean canServer = false;
    @LauncherAPI
    public boolean canUSR1 = false;
    @LauncherAPI
    public boolean canUSR2 = false;
    @LauncherAPI
    public boolean canUSR3 = false;
    @LauncherAPI
    public boolean canBot = false;

    public ClientPermissions(HInput input) throws IOException {
        canAdmin = input.readBoolean();
        canServer = input.readBoolean();
        canUSR1 = input.readBoolean();
        canUSR2 = input.readBoolean();
        canUSR3 = input.readBoolean();
        canBot = input.readBoolean();
    }

    public ClientPermissions() {
        canAdmin = false;
        canServer = false;
        canUSR1 = false;
        canUSR2 = false;
        canUSR3 = false;
        canBot = false;
    }

    public ClientPermissions(long data) {
        canAdmin = (data & (1)) != 0;
        canServer = (data & (1 << 1)) != 0;
        canUSR1 = (data & (1 << 2)) != 0;
        canUSR2 = (data & (1 << 3)) != 0;
        canUSR3 = (data & (1 << 4)) != 0;
        canBot = (data & (1 << 5)) != 0;
    }
    @LauncherAPI
    public long toLong()
    {
        long result = 0;
        result |= canAdmin ? 0 : 1;
        result |= canServer ? 0 : (1 << 1);
        result |= canUSR1 ? 0 : (1 << 2);
        result |= canUSR2 ? 0 : (1 << 3);
        result |= canUSR3 ? 0 : (1 << 4);
        result |= canBot ? 0 : (1 << 5);
        return result;
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
        output.writeBoolean(canUSR1);
        output.writeBoolean(canUSR2);
        output.writeBoolean(canUSR3);
        output.writeBoolean(canBot);
    }
}
