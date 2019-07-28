package pro.gravit.launcher.events.request;

import java.util.UUID;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;


public class LauncherRequestEvent extends RequestEvent {
    private static final UUID uuid = UUID.fromString("d54cc12a-4f59-4f23-9b10-f527fdd2e38f");
    @LauncherNetworkAPI
    public String url;
    @LauncherNetworkAPI
    public byte[] digest;
    @LauncherNetworkAPI
    public byte[] binary;

    public LauncherRequestEvent(boolean needUpdate, String url) {
        this.needUpdate = needUpdate;
        this.url = url;
    }

    @LauncherNetworkAPI
    public boolean needUpdate;

    public LauncherRequestEvent(boolean b, byte[] digest) {
        this.needUpdate = b;
        this.digest = digest;
    }

    public LauncherRequestEvent(byte[] binary, byte[] digest) { //Legacy support constructor
        this.binary = binary;
        this.digest = digest;
    }

    @Override
    public String getType() {
        return "launcher";
    }
}
