package ru.gravit.launchserver.response.update;

import java.io.IOException;
import java.util.Collection;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.logging.LogHelper;

public final class ProfilesResponse extends Response {

    public ProfilesResponse(LaunchServer server, long session, HInput input, HOutput output, String ip) {
        super(server, session, input, output, ip);
    }

    @Override
    public void reply() throws IOException {
        // Resolve launcher binary
        Client client = server.sessionManager.getClient(session);
        input.readBoolean();
        if (client.type == Client.Type.USER && !client.checkSign) {
            LogHelper.warning("User session: %d ip %s try get profiles", session, ip);
            requestError("Assess denied");
            return;
        }
        writeNoError(output);
        Collection<ClientProfile> profiles = server.getProfiles();
        output.writeLength(profiles.size(), 0);
        for (ClientProfile profile : profiles) {
            LogHelper.debug("Writted profile: %s", profile.getTitle());
            output.writeString(Launcher.gson.toJson(profile),0);
        }
    }
}
