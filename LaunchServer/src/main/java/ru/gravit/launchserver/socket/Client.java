package ru.gravit.launchserver.socket;

import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launchserver.auth.ClientPermissions;

public class Client {
    public long session;

    public long timestamp;
    public Type type;
    public ClientProfile profile;
    public boolean isAuth;
    public ClientPermissions permissions;
    public String username;

    public Client(long session) {
        this.session = session;
        timestamp = System.currentTimeMillis();
        type = Type.USER;
        isAuth = false;
        permissions = ClientPermissions.DEFAULT;
        username = "";
    }
    //Данные ваторизации
    public void up() {
        timestamp = System.currentTimeMillis();
    }
    public enum Type
    {
        SERVER,
        USER
    }
}
