package ru.gravit.launchserver.socket.websocket.json.admin;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;

public class ExecCommandResponse implements JsonResponseInterface {
    public String cmd;

    @Override
    public String getType() {
        return "cmdExec";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        if (!client.isAuth) {
            service.sendObject(ctx, new WebSocketService.ErrorResult("Access denied"));
            return;
        }
        if (!client.permissions.canAdmin) {
            service.sendObject(ctx, new WebSocketService.ErrorResult("Access denied"));
            return;
        }
        LaunchServer.server.commandHandler.eval(cmd, false);
        service.sendObject(ctx, new WebSocketService.SuccessResult("cmdExec"));
    }
}
