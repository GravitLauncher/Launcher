package ru.gravit.launcher.request.uuid;

import java.io.IOException;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;

public final class BatchProfileByUsernameRequest extends Request<PlayerProfile[]> {
    private final String[] usernames;

    @LauncherAPI
    public BatchProfileByUsernameRequest(LauncherConfig config, String... usernames) throws IOException {
        super(config);
        this.usernames = usernames.clone();
        IOHelper.verifyLength(this.usernames.length, SerializeLimits.MAX_BATCH_SIZE);
        for (String username : this.usernames)
			VerifyHelper.verifyUsername(username);
    }

    @LauncherAPI
    public BatchProfileByUsernameRequest(String... usernames) throws IOException {
        this(null, usernames);
    }

    @Override
    public Integer getType() {
        return RequestType.BATCH_PROFILE_BY_USERNAME.getNumber();
    }

    @Override
    protected PlayerProfile[] requestDo(HInput input, HOutput output) throws IOException {
        output.writeLength(usernames.length, SerializeLimits.MAX_BATCH_SIZE);
        for (String username : usernames) {
            output.writeString(username, SerializeLimits.MAX_LOGIN);
            output.writeString("", SerializeLimits.MAX_CLIENT);
        }
        output.flush();

        // Read profiles response
        PlayerProfile[] profiles = new PlayerProfile[usernames.length];
        for (int i = 0; i < profiles.length; i++)
			profiles[i] = input.readBoolean() ? new PlayerProfile(input) : null;

        // Return result
        return profiles;
    }
}
