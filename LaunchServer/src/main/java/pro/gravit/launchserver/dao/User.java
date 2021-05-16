package pro.gravit.launchserver.dao;

import pro.gravit.launcher.ClientPermissions;

import java.util.UUID;

@Deprecated
public interface User {
    String getUsername();

    ClientPermissions getPermissions();

    void setPermissions(ClientPermissions permissions);

    boolean verifyPassword(String password);

    void setPassword(String password);

    String getAccessToken();

    void setAccessToken(String accessToken);

    String getServerID();

    void setServerID(String serverID);

    UUID getUuid();

    void setUuid(UUID uuid);
}
