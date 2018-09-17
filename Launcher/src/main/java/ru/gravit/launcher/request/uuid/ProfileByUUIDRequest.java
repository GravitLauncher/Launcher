package ru.gravit.launcher.request.uuid;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.client.ClientLauncher;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;

public final class ProfileByUUIDRequest extends Request<PlayerProfile> {
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
    public Integer getType() {
        return RequestType.PROFILE_BY_UUID.getNumber();
    }

    @Override
    protected PlayerProfile requestDo(HInput input, HOutput output) throws IOException {
        output.writeUUID(uuid);
        output.writeString(ClientLauncher.title, SerializeLimits.MAX_CLIENT);
        output.flush();

        // Return profile
        return input.readBoolean() ? new PlayerProfile(input) : null;
    }
}
