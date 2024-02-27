package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.base.events.RequestEvent;

import java.util.UUID;

public class GetConnectUUIDRequestEvent extends RequestEvent {
    public UUID connectUUID;

    public GetConnectUUIDRequestEvent(UUID connectUUID) {
        this.connectUUID = connectUUID;
    }

    @Override
    public String getType() {
        return "getConnectUUID";
    }
}
