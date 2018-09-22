package ru.gravit.launchserver.socket;

public class Client {
    public long session;

    public long timestamp;

    public Client(long session) {
        this.session = session;
        timestamp = System.currentTimeMillis();
    }

    public void up() {
        timestamp = System.currentTimeMillis();
    }
}
