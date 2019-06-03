package pro.gravit.launchserver.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.CheckServerRequestEvent;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.websocket.json.SimpleResponse;
import pro.gravit.launchserver.websocket.json.profile.ProfileByUUIDResponse;
import pro.gravit.utils.HookException;
import pro.gravit.utils.helper.LogHelper;

public class CheckServerResponse extends SimpleResponse {
    public String serverID;
    public String username;
    public String client;

    @Override
    public String getType() {
        return "checkServer";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client pClient) {
        CheckServerRequestEvent result = new CheckServerRequestEvent();
        try {
            server.authHookManager.checkServerHook.hook(this, pClient);
            result.uuid = pClient.auth.handler.checkServer(username, serverID);
            if (result.uuid != null)
                result.playerProfile = ProfileByUUIDResponse.getProfile(server, result.uuid, username, client, pClient.auth.textureProvider);
            LogHelper.debug("checkServer: %s uuid: %s serverID: %s", result.playerProfile.username, result.uuid.toString(), serverID);
        } catch (AuthException | HookException e) {
            sendError(e.getMessage());
            return;
        } catch (Exception e) {
            LogHelper.error(e);
            sendError("Internal authHandler error");
            return;
        }
        sendResult(result);
    }

}
