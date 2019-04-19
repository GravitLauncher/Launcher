package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.RequestEvent;
import ru.gravit.launcher.hasher.HashedDir;

public class UpdateRequestEvent extends RequestEvent {
    @LauncherNetworkAPI
    public HashedDir hdir;
    @LauncherNetworkAPI
    public String url;

    @Override
    public String getType() {
        return "update";
    }

    public UpdateRequestEvent(HashedDir hdir) {
        this.hdir = hdir;
    }

    public UpdateRequestEvent(HashedDir hdir, String url) {
        this.hdir = hdir;
        this.url = url;
    }
}
