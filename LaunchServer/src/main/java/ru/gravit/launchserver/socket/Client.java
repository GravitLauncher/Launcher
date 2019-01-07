package ru.gravit.launchserver.socket;

import ru.gravit.launcher.ClientPermissions;
import ru.gravit.launcher.profiles.ClientProfile;

public class Client {
    public long session;

    public long timestamp;
    public Type type;
    public ClientProfile profile;
    public boolean isAuth;
    public boolean checkSign;
    public ClientPermissions permissions;
    public String username;

    public Client(long session) {
        this.session = session;
        timestamp = System.currentTimeMillis();
        type = Type.USER;
        isAuth = false;
        permissions = ClientPermissions.DEFAULT;
        username = "";
        checkSign = false;
    }

    //Данные ваторизации
    public void up() {
        timestamp = System.currentTimeMillis();
    }

    public enum Type {
        SERVER,
        USER
    }
}
