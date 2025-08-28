package pro.gravit.launcher.core.api.model;

public interface UserPermissions {
    boolean hasRole(String role);
    boolean hasPerm(String action);
}
