package ru.gravit.launchserver.response.auth;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;

public final class JoinServerResponse extends Response {

    public JoinServerResponse(LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        super(server, session, input, output, ip, clientData);
    }

    @Override
    public void reply() throws IOException {
        String username = VerifyHelper.verifyUsername(input.readString(SerializeLimits.MAX_LOGIN));
        String accessToken = SecurityHelper.verifyToken(input.readASCII(-SecurityHelper.TOKEN_STRING_LENGTH));
        String serverID = VerifyHelper.verifyServerID(input.readASCII(SerializeLimits.MAX_SERVERID)); // With minus sign
        if (!clientData.isAuth || clientData.type != Client.Type.USER) {
            requestError("Access denied");
            return;
        }
        // Try join server with auth handler
        debug("Username: '%s', Access token: %s, Server ID: %s", username, accessToken, serverID);
        boolean success;
        try {
            server.authHookManager.joinServerHook(username, accessToken, serverID);
            success = clientData.auth.handler.joinServer(username, accessToken, serverID);
        } catch (AuthException e) {
            requestError(e.getMessage());
            return;
        } catch (Exception e) {
            LogHelper.error(e);
            requestError("Internal auth handler error");
            return;
        }
        writeNoError(output);

        // Write response
        output.writeBoolean(success);
    }
}
