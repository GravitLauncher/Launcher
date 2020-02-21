package pro.gravit.launcher;

import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;

import java.io.IOException;
import java.util.StringJoiner;

public class ClientPermissions {
    public static final ClientPermissions DEFAULT = new ClientPermissions();
    @LauncherNetworkAPI
    public boolean canAdmin;
    @LauncherNetworkAPI
    public boolean canServer;
    @LauncherNetworkAPI
    public final boolean canUSR1;
    @LauncherNetworkAPI
    public final boolean canUSR2;
    @LauncherNetworkAPI
    public final boolean canUSR3;
    @LauncherNetworkAPI
    public boolean canBot;

    public ClientPermissions(HInput input) throws IOException {
        this(input.readLong());
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


    public long toLong() {
        long result = 0;
        result |= !canAdmin ? 0 : 1;
        result |= !canServer ? 0 : (1 << 1);
        result |= !canUSR1 ? 0 : (1 << 2);
        result |= !canUSR2 ? 0 : (1 << 3);
        result |= !canUSR3 ? 0 : (1 << 4);
        result |= !canBot ? 0 : (1 << 5);
        return result;
    }

    public static ClientPermissions getSuperuserAccount() {
        ClientPermissions perm = new ClientPermissions();
        perm.canServer = true;
        perm.canAdmin = true;
        return perm;
    }

    public void write(HOutput output) throws IOException {
        output.writeLong(toLong());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClientPermissions.class.getSimpleName() + "[", "]")
                .add("canAdmin=" + canAdmin)
                .add("canServer=" + canServer)
                .add("canUSR1=" + canUSR1)
                .add("canUSR2=" + canUSR2)
                .add("canUSR3=" + canUSR3)
                .add("canBot=" + canBot)
                .toString();
    }
}
