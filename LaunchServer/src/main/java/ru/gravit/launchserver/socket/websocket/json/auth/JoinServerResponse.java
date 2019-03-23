package ru.gravit.launchserver.socket.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launcher.events.request.JoinServerRequestEvent;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;
import ru.gravit.utils.helper.LogHelper;

public class JoinServerResponse implements JsonResponseInterface {
    public String serverID;
    public String accessToken;
    public String username;

    @Override
    public String getType() {
        return "joinServer";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) {
        boolean success;
        try {
            success = client.auth.handler.joinServer(username, accessToken, serverID);
        } catch (AuthException e) {
            service.sendObject(ctx, new ErrorRequestEvent(e.getMessage()));
            return;
        } catch (Exception e) {
            LogHelper.error(e);
            service.sendObject(ctx, new ErrorRequestEvent("Internal authHandler error"));
            return;
        }
        service.sendObject(ctx, new JoinServerRequestEvent(success));
    }

}
