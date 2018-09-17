package ru.gravit.launchserver.response.update;

import java.io.IOException;
import java.util.Collection;

import ru.gravit.launcher.helper.LogHelper;
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
        input.readBoolean();
        writeNoError(output);
        Collection<SignedObjectHolder<ClientProfile>> profiles = server.getProfiles();
        output.writeLength(profiles.size(), 0);
        for (SignedObjectHolder<ClientProfile> profile : profiles) {
            LogHelper.debug("Writted profile: %s",profile.object.getTitle());
            profile.write(output);
        }
    }
}
