package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.hasher.HashedDir;

public class UpdateRequestEvent extends RequestEvent {
    @LauncherNetworkAPI
    public final HashedDir hdir;
    @LauncherNetworkAPI
    public final boolean zip;
    @LauncherNetworkAPI
    public String url;
    @LauncherNetworkAPI
    public boolean fullDownload;

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

    @Override
    public String getType() {
        return "update";
    }
}
