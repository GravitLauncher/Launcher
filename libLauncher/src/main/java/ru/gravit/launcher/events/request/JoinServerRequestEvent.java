package ru.gravit.launcher.events.request;

public class JoinServerRequestEvent {
    public String type = "success";
    public String requesttype = "checkServer";

    public JoinServerRequestEvent(boolean allow) {
        this.allow = allow;
    }

    public boolean allow;
}
