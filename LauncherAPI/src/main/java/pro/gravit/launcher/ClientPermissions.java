package pro.gravit.launcher;

import pro.gravit.launcher.serialize.HInput;

import java.io.IOException;
import java.util.*;

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
    private List<String> perms;

    private transient List<PermissionPattern> available;

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
        perm.addPerm("*");
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
        if (perms == null) {
            perms = new ArrayList<>(0);

        }
        available = new ArrayList<>(perms.size());
        for (String a : perms) {
            available.add(new PermissionPattern(a));
        }
        if (permissions != 0) {
            if (isPermission(PermissionConsts.ADMIN)) {
                roles.add("ADMIN");
                available.add(new PermissionPattern("*"));
            }
        }
    }

    public boolean hasPerm(String action) {
        if (available == null) {
            compile();
        }
        for (PermissionPattern p : available) {
            if (p.match(action)) {
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

    public void addPerm(String perm) {
        if (perms == null) {
            perms = new ArrayList<>(1);
        }
        perms.add(perm);
        if(available == null) {
            available = new ArrayList<>(1);
        }
        available.add(new PermissionPattern(perm));
    }

    public void removePerm(String action) {
        if (perms == null) {
            return;
        }
        if(available == null) {
            return;
        }
        perms.remove(action);
        available.remove(new PermissionPattern(action));
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPerms() {
        return perms;
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
        return "ClientPermissions{" +
                "roles=" + String.join(", ", roles == null ? Collections.emptyList() : roles) +
                ", actions=" + String.join(", ", perms == null ? Collections.emptyList() : perms) +
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

    public static class PermissionPattern {
        private final String[] parts;
        private final int priority;

        public PermissionPattern(String pattern) {
            List<String> prepare = new ArrayList<>();
            for(int i=0;true;) {
                int pos = pattern.indexOf("*", i);
                if(pos >= 0) {
                    prepare.add(pattern.substring(i, pos));
                    i = pos+1;
                } else {
                    prepare.add(pattern.substring(i));
                    break;
                }
            }
            priority = prepare.size() - 1;
            parts = prepare.toArray(new String[0]);
        }

        public int getPriority() {
            return priority;
        }

        public boolean match(String str) {
            if(parts.length == 0) {
                return true;
            }
            if(parts.length == 1) {
                return parts[0].equals(str);
            }
            int offset = 0;
            if(!str.startsWith(parts[0])) {
                return false;
            }
            if(!str.endsWith(parts[parts.length-1])) {
                return false;
            }
            for(int i=1;i<parts.length-1;++i) {
                int pos = str.indexOf(parts[i], offset);
                if(pos >= 0) {
                    offset = pos+1;
                } else {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PermissionPattern that = (PermissionPattern) o;
            return priority == that.priority && Arrays.equals(parts, that.parts);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(priority);
            result = 31 * result + Arrays.hashCode(parts);
            return result;
        }
    }
}
