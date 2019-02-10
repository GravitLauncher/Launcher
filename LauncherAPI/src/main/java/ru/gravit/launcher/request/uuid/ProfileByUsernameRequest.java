package ru.gravit.launcher.request.uuid;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.events.request.ProfileByUsernameRequestEvent;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;

public final class ProfileByUsernameRequest extends Request<ProfileByUsernameRequestEvent> {
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
    public Integer getLegacyType() {
        return RequestType.PROFILE_BY_USERNAME.getNumber();
    }

    @Override
    protected ProfileByUsernameRequestEvent requestDo(HInput input, HOutput output) throws IOException {
        output.writeString(username, SerializeLimits.MAX_LOGIN);
        output.writeString(Launcher.profile.getTitle(), SerializeLimits.MAX_CLIENT);
        output.flush();
        // Return profile
        return input.readBoolean() ? new ProfileByUsernameRequestEvent(new PlayerProfile(input)) : null;
    }
}
