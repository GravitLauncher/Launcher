package ru.gravit.launcher.events.request;

public class LauncherRequestEvent {
    public String type = "success";
    public String requesttype = "launcherUpdate";
    public String url;

    public LauncherRequestEvent(boolean needUpdate, String url) {
        this.needUpdate = needUpdate;
        this.url = url;
    }

    public boolean needUpdate;
}
