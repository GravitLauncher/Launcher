package ru.gravit.launchserver.response.update;

import java.io.IOException;
import java.util.Collection;

import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.Response;

public final class ProfilesResponse extends Response {

    public ProfilesResponse(LaunchServer server, long session, HInput input, HOutput output, String ip) {
        super(server, session, input, output, ip);
    }

    @Override
    public void reply() throws IOException {
        // Resolve launcher binary
        Client client = server.sessionManager.getClient(session);
        input.readBoolean();
        if(client.type == Client.Type.USER) {
            LogHelper.warning("User session: %d ip %s try get profiles",session,ip);
            requestError("Assess denied");
            return;
        }
        writeNoError(output);
        Collection<SignedObjectHolder<ClientProfile>> profiles = server.getProfiles();
        output.writeLength(profiles.size(), 0);
        for (SignedObjectHolder<ClientProfile> profile : profiles) {
            LogHelper.debug("Writted profile: %s", profile.object.getTitle());
            profile.write(output);
        }
    }
}
