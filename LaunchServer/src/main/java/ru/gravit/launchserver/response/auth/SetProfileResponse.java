package ru.gravit.launchserver.response.auth;

import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.Client;

import java.util.Collection;

public class SetProfileResponse extends Response {
    public SetProfileResponse(LaunchServer server, long session, HInput input, HOutput output, String ip) {
        super(server, session, input, output, ip);
    }

    @Override
    public void reply() throws Exception {
        String client = input.readString(SerializeLimits.MAX_CLIENT);
        Client clientData = server.sessionManager.getClient(session);
        if (!clientData.isAuth) requestError("You not auth");
        Collection<ClientProfile> profiles = server.getProfiles();
        for (ClientProfile p : profiles) {
            if (p.getTitle().equals(client)) {
                if (!p.isWhitelistContains(clientData.username)) {
                    requestError(server.config.whitelistRejectString);
                    return;
                }
                clientData.profile = p;
                writeNoError(output);
                output.writeBoolean(true);
                break;
            }
        }
    }
}
