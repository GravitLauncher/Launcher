package ru.gravit.launcher.request.uuid;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.events.request.ProfileByUUIDRequestEvent;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public final class ProfileByUUIDRequest extends Request<ProfileByUUIDRequestEvent> {
    private final UUID uuid;

    @LauncherAPI
    public ProfileByUUIDRequest(LauncherConfig config, UUID uuid) {
        super(config);
        this.uuid = Objects.requireNonNull(uuid, "uuid");
    }

    @LauncherAPI
    public ProfileByUUIDRequest(UUID uuid) {
        this(null, uuid);
    }

    @Override
    public Integer getLegacyType() {
        return RequestType.PROFILE_BY_UUID.getNumber();
    }

    @Override
    protected ProfileByUUIDRequestEvent requestDo(HInput input, HOutput output) throws IOException {
        output.writeUUID(uuid);
        output.writeString(Launcher.profile.getTitle(), SerializeLimits.MAX_CLIENT);
        output.flush();

        // Return profile
        return input.readBoolean() ? new ProfileByUUIDRequestEvent(new PlayerProfile(input)) : null;
    }
}
