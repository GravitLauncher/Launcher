package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;

import java.util.HashSet;
import java.util.UUID;


public class UpdateListRequestEvent extends RequestEvent {
    @SuppressWarnings("unused")
    private static final UUID uuid = UUID.fromString("5fa836ae-6b61-401c-96ac-d8396f07ec6b");
    @LauncherNetworkAPI
    public final HashSet<String> dirs;

    public UpdateListRequestEvent(HashSet<String> dirs) {
        this.dirs = dirs;
    }

    @Override
    public String getType() {
        return "updateList";
    }
}
