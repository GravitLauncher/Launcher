package ru.gravit.launcher.request.uuid;

import java.io.IOException;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.client.ClientLauncher;
import ru.gravit.launcher.helper.VerifyHelper;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;

public final class ProfileByUsernameRequest extends Request<PlayerProfile> {
    private final String username;

    @LauncherAPI
    public ProfileByUsernameRequest(LauncherConfig config, String username) {
        super(config);
        this.username = VerifyHelper.verifyUsername(username);
    }

    @LauncherAPI
    public ProfileByUsernameRequest(String username) {
        this(null, username);
    }

    @Override
    public Integer getType() {
        return RequestType.PROFILE_BY_USERNAME.getNumber();
    }

    @Override
    protected PlayerProfile requestDo(HInput input, HOutput output) throws IOException {
        output.writeString(username, SerializeLimits.MAX_LOGIN);
        output.writeString(ClientLauncher.title, SerializeLimits.MAX_CLIENT);
        output.flush();
        // Return profile
        return input.readBoolean() ? new PlayerProfile(input) : null;
    }
}
