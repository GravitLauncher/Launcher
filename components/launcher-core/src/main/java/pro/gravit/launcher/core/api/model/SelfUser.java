package pro.gravit.launcher.core.api.model;

public interface SelfUser extends User {
    String getAccessToken();
    UserPermissions getPermissions();
}
