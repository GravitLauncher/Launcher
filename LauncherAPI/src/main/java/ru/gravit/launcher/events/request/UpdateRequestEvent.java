package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.RequestEvent;
import ru.gravit.launcher.hasher.HashedDir;

public class UpdateRequestEvent extends RequestEvent {
    @LauncherNetworkAPI
    public HashedDir hdir;
    @LauncherNetworkAPI
    public String url;
    @LauncherNetworkAPI
    public boolean zip;

    @Override
    public String getType() {
        return "update";
    }

    public UpdateRequestEvent(HashedDir hdir) {
        this.hdir = hdir;
        this.zip = false;
    }

    public UpdateRequestEvent(HashedDir hdir, String url) {
        this.hdir = hdir;
        this.url = url;
        this.zip = false;
    }

    public UpdateRequestEvent(HashedDir hdir, String url, boolean zip) {
        this.hdir = hdir;
        this.url = url;
        this.zip = zip;
    }
}
