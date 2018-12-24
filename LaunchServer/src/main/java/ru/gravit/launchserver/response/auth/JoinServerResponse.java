package ru.gravit.launchserver.response.auth;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.response.Response;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;

public final class JoinServerResponse extends Response {

    public JoinServerResponse(LaunchServer server, long session, HInput input, HOutput output, String ip) {
        super(server, session, input, output, ip);
    }

    @Override
    public void reply() throws IOException {
        String username = VerifyHelper.verifyUsername(input.readString(SerializeLimits.MAX_LOGIN));
        String accessToken = SecurityHelper.verifyToken(input.readASCII(-SecurityHelper.TOKEN_STRING_LENGTH));
        String serverID = VerifyHelper.verifyServerID(input.readASCII(SerializeLimits.MAX_SERVERID)); // With minus sign

        // Try join server with auth handler
        debug("Username: '%s', Access token: %s, Server ID: %s", username, accessToken, serverID);
        boolean success;
        try {
            success = server.config.authHandler.joinServer(username, accessToken, serverID);
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
