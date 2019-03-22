package ru.gravit.launchserver.response.auth;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.response.profile.ProfileByUUIDResponse;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.util.UUID;

public final class CheckServerResponse extends Response {

    public CheckServerResponse(LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        super(server, session, input, output, ip, clientData);
    }

    @Override
    public void reply() throws IOException {
        String username = VerifyHelper.verifyUsername(input.readString(SerializeLimits.MAX_LOGIN));
        String serverID = VerifyHelper.verifyServerID(input.readASCII(41)); // With minus sign
        String client = input.readString(SerializeLimits.MAX_CLIENT);
        debug("Username: %s, Server ID: %s", username, serverID);
        if (!clientData.isAuth || clientData.type != Client.Type.SERVER) {
            requestError("Access denied");
            return;
        }
        // Try check server with auth handler
        UUID uuid;
        try {
            server.authHookManager.checkServerHook(username, serverID);
            uuid = clientData.auth.handler.checkServer(username, serverID);
        } catch (AuthException e) {
            requestError(e.getMessage());
            return;
        } catch (Exception e) {
            LogHelper.error(e);
            requestError("Internal auth handler error");
            return;
        }
        writeNoError(output);

        // Write profile and UUID
        output.writeBoolean(uuid != null);
        if (uuid != null)
            ProfileByUUIDResponse.getProfile(server, uuid, username, client, clientData.auth.textureProvider).write(output);
    }
}
