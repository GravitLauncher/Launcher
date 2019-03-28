package ru.gravit.launchserver.response.profile;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthProviderPair;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.util.UUID;

public final class ProfileByUsernameResponse extends Response {

    public static void writeProfile(LaunchServer server, HOutput output, String username, String client, AuthProviderPair pair) throws IOException {
        UUID uuid = pair.handler.usernameToUUID(username);
        if (uuid == null) {
            output.writeBoolean(false);
            return;
        }

        // Write profile
        output.writeBoolean(true);
        ProfileByUUIDResponse.getProfile(server, uuid, username, client, pair.textureProvider).write(output);
    }

    public ProfileByUsernameResponse(LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        super(server, session, input, output, ip, clientData);
    }

    @Override
    public void reply() throws IOException {
        String username = VerifyHelper.verifyUsername(input.readString(64));
        debug("Username: " + username);
        String client = input.readString(SerializeLimits.MAX_CLIENT);
        // Write response
        writeProfile(server, output, username, client, clientData.auth);
    }
}
