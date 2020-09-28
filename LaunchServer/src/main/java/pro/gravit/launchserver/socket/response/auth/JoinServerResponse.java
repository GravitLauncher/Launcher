package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.JoinServerRequestEvent;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.protect.interfaces.JoinServerProtectHandler;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.utils.HookException;
import pro.gravit.utils.helper.LogHelper;

public class JoinServerResponse extends SimpleResponse {
    public String serverID;
    public String accessToken;
    public String username;

    @Override
    public String getType() {
        return "joinServer";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (!client.isAuth || client.type != AuthResponse.ConnectTypes.CLIENT) {
            sendError("Permissions denied");
            return;
        }
        if (username == null || accessToken == null || serverID == null) {
            sendError("Invalid request");
            return;
        }
        boolean success;
        try {
            server.authHookManager.joinServerHook.hook(this, client);
            if (server.config.protectHandler instanceof JoinServerProtectHandler) {
                success = ((JoinServerProtectHandler) server.config.protectHandler).onJoinServer(serverID, username, client);
                if (!success) {
                    sendResult(new JoinServerRequestEvent(false));
                    return;
                }
            }
            if (client.auth == null) {
                LogHelper.warning("Client auth is null. Using default.");
                success = server.config.getAuthProviderPair().handler.joinServer(username, accessToken, serverID);
            } else success = client.auth.handler.joinServer(username, accessToken, serverID);
            if (LogHelper.isDebugEnabled()) {
                LogHelper.debug("joinServer: %s accessToken: %s serverID: %s", username, accessToken, serverID);
            }
        } catch (AuthException | HookException | SecurityException e) {
            sendError(e.getMessage());
            return;
        } catch (Exception e) {
            LogHelper.error(e);
            sendError("Internal authHandler error");
            return;
        }
        sendResult(new JoinServerRequestEvent(success));
    }

}
