package ru.gravit.launcher.request.uuid;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.request.BatchProfileByUsernameRequestEvent;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;

public final class BatchProfileByUsernameRequest extends Request<BatchProfileByUsernameRequestEvent> implements RequestInterface {
    class Entry {
        @LauncherNetworkAPI
        String username;
        @LauncherNetworkAPI
        String client;
    }
    @LauncherNetworkAPI
    private final Entry[] list;

    @LauncherAPI
    public BatchProfileByUsernameRequest(LauncherConfig config, String... usernames) throws IOException {
        super(config);
        this.list = new Entry[usernames.length];
        for(int i=0;i<usernames.length;++i)
        {
            this.list[i].client = "";
            this.list[i].username = usernames[i];
        }
        IOHelper.verifyLength(usernames.length, SerializeLimits.MAX_BATCH_SIZE);
        for (String username : usernames)
            VerifyHelper.verifyUsername(username);
    }

    @LauncherAPI
    public BatchProfileByUsernameRequest(String... usernames) throws IOException {
        this(null, usernames);
    }
    @Override
    public BatchProfileByUsernameRequestEvent requestWebSockets() throws IOException, InterruptedException {
        return (BatchProfileByUsernameRequestEvent) LegacyRequestBridge.sendRequest(this);
    }

    @Override
    public Integer getLegacyType() {
        return RequestType.BATCH_PROFILE_BY_USERNAME.getNumber();
    }

    @Override
    protected BatchProfileByUsernameRequestEvent requestDo(HInput input, HOutput output) throws IOException {
        output.writeLength(list.length, SerializeLimits.MAX_BATCH_SIZE);
        for (Entry username : list) {
            output.writeString(username.username, SerializeLimits.MAX_LOGIN);
            output.writeString(username.client, SerializeLimits.MAX_CLIENT);
        }
        output.flush();

        // Read profiles response
        PlayerProfile[] profiles = new PlayerProfile[list.length];
        for (int i = 0; i < profiles.length; i++)
            profiles[i] = input.readBoolean() ? new PlayerProfile(input) : null;

        // Return result
        return new BatchProfileByUsernameRequestEvent(profiles);
    }

    @Override
    public String getType() {
        return "batchProfileByUsername";
    }
}
