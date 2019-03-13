package ru.gravit.launchserver.response.update;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.Collection;

public final class ProfilesResponse extends Response {

    public ProfilesResponse(LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        super(server, session, input, output, ip, clientData);
    }

    @Override
    public void reply() throws IOException {
        // Resolve launcher binary
        input.readBoolean();
        if (clientData.type == Client.Type.USER && !clientData.checkSign) {
            LogHelper.warning("User session: %d ip %s try get profiles", session, ip);
            requestError("Access denied");
            return;
        }
        writeNoError(output);
        Collection<ClientProfile> profiles = server.getProfiles();
        output.writeLength(profiles.size(), 0);
        for (ClientProfile profile : profiles) {
            LogHelper.debug("Writted profile: %s", profile.getTitle());
            output.writeString(Launcher.gson.toJson(profile), 0);
        }
    }
}
