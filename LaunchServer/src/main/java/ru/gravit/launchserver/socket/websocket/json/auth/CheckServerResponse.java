package ru.gravit.launchserver.socket.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.CheckServerRequestEvent;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.response.profile.ProfileByUUIDResponse;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;
import ru.gravit.utils.helper.LogHelper;

public class CheckServerResponse implements JsonResponseInterface {
    public String serverID;
    public String username;
    public String client;

    @Override
    public String getType() {
        return "checkServer";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client pClient) {
        CheckServerRequestEvent result = new CheckServerRequestEvent();
        try {
            result.uuid = pClient.auth.handler.checkServer(username, serverID);
            if(result.uuid != null)
                result.playerProfile = ProfileByUUIDResponse.getProfile(LaunchServer.server,result.uuid,username,client, pClient.auth.textureProvider);
        } catch (AuthException e) {
            service.sendObject(ctx, new ErrorRequestEvent(e.getMessage()));
            return;
        } catch (Exception e) {
            LogHelper.error(e);
            service.sendObject(ctx, new ErrorRequestEvent("Internal authHandler error"));
            return;
        }
        service.sendObject(ctx, new CheckServerRequestEvent());
    }

}
