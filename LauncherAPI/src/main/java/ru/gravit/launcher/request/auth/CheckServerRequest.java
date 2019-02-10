package ru.gravit.launcher.request.auth;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.events.request.CheckServerRequestEvent;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;

public final class CheckServerRequest extends Request<CheckServerRequestEvent> {
    private final String username;
    private final String serverID;

    @LauncherAPI
    public CheckServerRequest(LauncherConfig config, String username, String serverID) {
        super(config);
        this.username = VerifyHelper.verifyUsername(username);
        this.serverID = VerifyHelper.verifyServerID(serverID);
    }

    @LauncherAPI
    public CheckServerRequest(String username, String serverID) {
        this(null, username, serverID);
    }

    @Override
    public Integer getLegacyType() {
        return RequestType.CHECK_SERVER.getNumber();
    }

    @Override
    protected CheckServerRequestEvent requestDo(HInput input, HOutput output) throws IOException {
        output.writeString(username, SerializeLimits.MAX_LOGIN);
        output.writeASCII(serverID, SerializeLimits.MAX_SERVERID); // 1 char for minus sign
        if (Launcher.profile == null) {
            LogHelper.error("Profile is null. Title is not net.");
            output.writeString("", SerializeLimits.MAX_CLIENT);
        } else
            output.writeString(Launcher.profile.getTitle(), SerializeLimits.MAX_CLIENT);
        output.flush();

        // Read response
        readError(input);
        return input.readBoolean() ? new CheckServerRequestEvent(new PlayerProfile(input)) : null;
    }
}
