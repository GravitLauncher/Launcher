package ru.gravit.launchserver.socket;

import ru.gravit.launcher.ClientPermissions;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthProviderPair;
import ru.gravit.utils.helper.LogHelper;

public class Client {
    public long session;
    public String auth_id;
    public long timestamp;
    public Type type;
    public ClientProfile profile;
    public boolean isAuth;
    public boolean checkSign;
    public boolean isSecure;
    public ClientPermissions permissions;
    public String username;
    public LogHelper.OutputEnity logOutput;

    public transient AuthProviderPair auth;

    public Client(long session) {
        this.session = session;
        timestamp = System.currentTimeMillis();
        type = Type.USER;
        isAuth = false;
        permissions = ClientPermissions.DEFAULT;
        username = "";
        checkSign = false;
    }

    //Данные авторизации
    public void up() {
        timestamp = System.currentTimeMillis();
    }

    public void updateAuth() {
        if (!isAuth) return;
        if (auth_id.isEmpty()) auth = LaunchServer.server.config.getAuthProviderPair();
        else auth = LaunchServer.server.config.getAuthProviderPair(auth_id);
    }

    public enum Type {
        SERVER,
        USER
    }
}
