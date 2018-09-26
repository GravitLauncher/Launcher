package ru.gravit.launchserver.socket;

import ru.gravit.launcher.profiles.ClientProfile;

public class Client {
    public long session;

    public long timestamp;
    public Type type;
    public ClientProfile profile;
    public boolean isAuth;

    public Client(long session) {
        this.session = session;
        timestamp = System.currentTimeMillis();
        type = Type.USER;
        isAuth = false;
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
