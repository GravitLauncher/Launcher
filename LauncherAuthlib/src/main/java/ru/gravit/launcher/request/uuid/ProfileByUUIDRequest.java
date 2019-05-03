package ru.gravit.launcher.request.uuid;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.ProfileByUUIDRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.RequestInterface;

import java.util.Objects;
import java.util.UUID;

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
