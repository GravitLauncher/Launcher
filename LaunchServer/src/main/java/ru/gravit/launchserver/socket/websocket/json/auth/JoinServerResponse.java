package ru.gravit.launchserver.socket.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;
import ru.gravit.utils.logging.LogHelper;

public class JoinServerResponse implements JsonResponseInterface {
    public String serverID;
    public String accessToken;
    public String username;

    @Override
    public String getType() {
        return "joinServer";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        boolean success;
        try {
            success = LaunchServer.server.config.authHandler.joinServer(username, accessToken, serverID);
        } catch (AuthException e) {
            service.sendObject(ctx, new WebSocketService.ErrorResult(e.getMessage()));
            return;
        } catch (Exception e) {
            LogHelper.error(e);
            service.sendObject(ctx, new WebSocketService.ErrorResult("Internal authHandler error"));
            return;
        }
        service.sendObject(ctx, new Result(success));
    }

    public class Result {
        public String type = "success";
        public String requesttype = "checkServer";

        public Result(boolean allow) {
            this.allow = allow;
        }

        public boolean allow;
    }
}
