package pro.gravit.launcher;

import pro.gravit.launcher.serialize.HInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ClientPermissions {
    public static final ClientPermissions DEFAULT = new ClientPermissions();
    @LauncherNetworkAPI
    @Deprecated
    public long permissions;
    @LauncherNetworkAPI
    @Deprecated
    public long flags;
    @LauncherNetworkAPI
    private List<String> roles;
    @LauncherNetworkAPI
    private List<String> actions;

    private transient List<Pattern> available;

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
        perm.addAction("*");
        return perm;
    }

    public long toLong() {
        return permissions;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public synchronized void compile() {
        if (available != null) {
            return;
        }
        available = new ArrayList<>(actions.size());
        for (String a : actions) {
            available.add(Pattern.compile(a));
        }
        if (permissions != 0) {
            if (isPermission(PermissionConsts.ADMIN)) {
                roles.add("ADMIN");
                available.add(Pattern.compile(".*"));
            }
        }
    }

    public boolean hasAction(String action) {
        if (available == null) {
            compile();
        }
        for (Pattern p : available) {
            if (p.matcher(action).matches()) {
                return true;
            }
        }
        return false;
    }

    public void addRole(String role) {
        if (roles == null) {
            roles = new ArrayList<>(1);
        }
        roles.add(role);
    }

    public void addAction(String action) {
        if (actions == null) {
            actions = new ArrayList<>();
        }
        actions.add(action);
        available.add(Pattern.compile(action));
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getActions() {
        return actions;
    }

    //Read methods
    @Deprecated
    public final boolean isPermission(PermissionConsts con) {
        return (permissions & con.mask) != 0;
    }

    @Deprecated
    public final boolean isPermission(long mask) {
        return (permissions & mask) != 0;
    }

    @Deprecated
    public final boolean isFlag(FlagConsts con) {
        return (flags & con.mask) != 0;
    }

    @Deprecated
    public final boolean isFlag(long mask) {
        return (flags & mask) != 0;
    }

    //Write methods
    @Deprecated
    public final void setPermission(PermissionConsts con, boolean value) {
        if (value) this.permissions |= con.mask;
        else this.permissions &= ~con.mask;
    }

    @Deprecated
    public final void setPermission(long mask, boolean value) {
        if (value) this.permissions |= mask;
        else this.permissions &= ~mask;
    }

    @Deprecated
    public final void setFlag(FlagConsts con, boolean value) {
        if (value) this.flags |= con.mask;
        else this.flags &= ~con.mask;
    }

    @Deprecated
    public final void setFlag(long mask, boolean value) {
        if (value) this.flags |= mask;
        else this.flags &= ~mask;
    }

    @Override
    public String toString() {
        if (roles != null || actions != null) {
            return "ClientPermissions{" +
                    "roles=" + String.join(", ", roles == null ? Collections.emptyList() : roles) +
                    ", actions=" + String.join(", ", actions == null ? Collections.emptyList() : actions) +
                    '}';
        }
        return "ClientPermissions{" +
                "permissions=" + permissions +
                ", flags=" + flags +
                '}';
    }

    @Deprecated
    public enum PermissionConsts {
        ADMIN(0x01),
        MANAGEMENT(0x02);
        public final long mask;

        PermissionConsts(long mask) {
            this.mask = mask;
        }
    }

    @Deprecated
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
