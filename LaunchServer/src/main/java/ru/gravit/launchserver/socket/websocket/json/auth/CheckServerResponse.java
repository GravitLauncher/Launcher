package ru.gravit.launchserver.socket.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;
import ru.gravit.utils.helper.LogHelper;

import java.util.UUID;

public class CheckServerResponse implements JsonResponseInterface {
    public String serverID;
    public String username;
    @Override
    public String getType() {
        return "checkServer";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        UUID uuid;
        try {
            uuid = LaunchServer.server.config.authHandler[0].checkServer(username, serverID);
        } catch (AuthException e) {
            service.sendObject(ctx,new WebSocketService.ErrorResult(e.getMessage()));
            return;
        } catch (Exception e) {
            LogHelper.error(e);
            service.sendObject(ctx,new WebSocketService.ErrorResult("Internal authHandler error"));
            return;
        }
        service.sendObject(ctx,new Result());
    }
    public class Result
    {
        public String type = "success";
        public String requesttype = "checkServer";
    }
}
