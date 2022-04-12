package pro.gravit.launcher;

import pro.gravit.launcher.serialize.HInput;

import java.io.IOException;
import java.util.*;

public class ClientPermissions {
    public static final ClientPermissions DEFAULT = new ClientPermissions();
    @LauncherNetworkAPI
    private List<String> roles;
    @LauncherNetworkAPI
    private List<String> perms;

    private transient List<PermissionPattern> available;

    public ClientPermissions() {

    }

    public ClientPermissions(List<String> roles, List<String> permissions) {
        this.roles = new ArrayList<>(roles);
        this.perms = new ArrayList<>(permissions);
    }

    public static ClientPermissions getSuperuserAccount() {
        ClientPermissions perm = new ClientPermissions();
        perm.addPerm("*");
        return perm;
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

    @Override
    public String toString() {
        return "ClientPermissions{" +
                "roles=" + String.join(", ", roles == null ? Collections.emptyList() : roles) +
                ", actions=" + String.join(", ", perms == null ? Collections.emptyList() : perms) +
                '}';
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
