package pro.gravit.launcher;

import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;

import java.io.IOException;

public class ClientPermissions {
    public static final ClientPermissions DEFAULT = new ClientPermissions();
    @LauncherNetworkAPI
    public long permissions;
    @LauncherNetworkAPI
    public long flags;

    public ClientPermissions(HInput input) throws IOException {
        this(input.readLong());
    }

    public ClientPermissions() {

    }

    public ClientPermissions(long permissions) {
        this.permissions = permissions;
    }

    public ClientPermissions(long permissions, long flags) {
        this.permissions = permissions;
        this.flags = flags;
    }

    public static ClientPermissions getSuperuserAccount() {
        ClientPermissions perm = new ClientPermissions();
        perm.setPermission(PermissionConsts.ADMIN, true);
        return perm;
    }

    public long toLong() {
        return permissions;
    }

    @Deprecated
    public void write(HOutput output) throws IOException {
        output.writeLong(toLong());
    }

    //Read methods
    public final boolean isPermission(PermissionConsts con) {
        return (permissions & con.mask) != 0;
    }

    public final boolean isPermission(long mask) {
        return (permissions & mask) != 0;
    }

    public final boolean isFlag(FlagConsts con) {
        return (flags & con.mask) != 0;
    }

    public final boolean isFlag(long mask) {
        return (flags & mask) != 0;
    }

    //Write methods
    public final void setPermission(PermissionConsts con, boolean value) {
        if (value) this.permissions |= con.mask;
        else this.permissions &= ~con.mask;
    }

    public final void setPermission(long mask, boolean value) {
        if (value) this.permissions |= mask;
        else this.permissions &= ~mask;
    }

    public final void setFlag(FlagConsts con, boolean value) {
        if (value) this.flags |= con.mask;
        else this.flags &= ~con.mask;
    }

    public final void setFlag(long mask, boolean value) {
        if (value) this.flags |= mask;
        else this.flags &= ~mask;
    }

    @Override
    public String toString() {
        return "ClientPermissions{" +
                "permissions=" + permissions +
                ", flags=" + flags +
                '}';
    }

    public enum PermissionConsts {
        ADMIN(0x01),
        MANAGEMENT(0x02);
        public final long mask;

        PermissionConsts(long mask) {
            this.mask = mask;
        }
    }


    public enum FlagConsts {
        SYSTEM(0x01),
        BANNED(0x02),
        UNTRUSTED(0x04),
        HIDDEN(0x08);
        public final long mask;

        FlagConsts(long mask) {
            this.mask = mask;
        }
    }
}
