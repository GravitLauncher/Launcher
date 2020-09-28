package pro.gravit.launcher.request.uuid;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.ProfileByUUIDRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.WebSocketRequest;

import java.util.Objects;
import java.util.UUID;

public final class ProfileByUUIDRequest extends Request<ProfileByUUIDRequestEvent> implements WebSocketRequest {
    @LauncherNetworkAPI
    public final UUID uuid;


    public ProfileByUUIDRequest(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
    }

    @Override
    public String getType() {
        return "profileByUUID";
    }
}
