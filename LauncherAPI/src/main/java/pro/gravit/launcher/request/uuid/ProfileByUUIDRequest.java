package pro.gravit.launcher.request.uuid;

import java.util.Objects;
import java.util.UUID;

import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.ProfileByUUIDRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.RequestInterface;

public final class ProfileByUUIDRequest extends Request<ProfileByUUIDRequestEvent> implements RequestInterface {
    @LauncherNetworkAPI
    private final UUID uuid;

    @LauncherAPI
    public ProfileByUUIDRequest(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
    }

    @Override
    public String getType() {
        return "profileByUUID";
    }
}
