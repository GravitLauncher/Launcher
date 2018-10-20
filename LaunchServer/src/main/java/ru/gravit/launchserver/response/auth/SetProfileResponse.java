package ru.gravit.launchserver.response.auth;

import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
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
        if(!clientData.isAuth) requestError("You not auth");
        Collection<SignedObjectHolder<ClientProfile>> profiles = server.getProfiles();
        for (SignedObjectHolder<ClientProfile> p : profiles) {
            if (p.object.getTitle().equals(client)) {
                if (!p.object.isWhitelistContains(clientData.username)) {
                    requestError(server.config.whitelistRejectString);
                    return;
                }
                clientData.profile = p.object;
                output.writeBoolean(true);
                break;
            }
        }
    }
}
