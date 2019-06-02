package pro.gravit.launchserver.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.websocket.json.SimpleResponse;
import pro.gravit.launcher.events.request.JoinServerRequestEvent;
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
        boolean success;
        try {
            server.authHookManager.joinServerHook.hook(this, client);
            if (client.auth == null) {
                LogHelper.warning("Client auth is null. Using default.");
                success = LaunchServer.server.config.getAuthProviderPair().handler.joinServer(username, accessToken, serverID);
            } else success = client.auth.handler.joinServer(username, accessToken, serverID);
            LogHelper.debug("joinServer: %s accessToken: %s serverID: %s", username, accessToken, serverID);
        } catch (AuthException | HookException e) {
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
