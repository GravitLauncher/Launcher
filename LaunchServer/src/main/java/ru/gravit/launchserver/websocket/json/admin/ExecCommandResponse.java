package ru.gravit.launchserver.websocket.json.admin;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launcher.events.request.ExecCommandRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.WebSocketService;
import ru.gravit.launchserver.websocket.json.JsonResponseInterface;
import ru.gravit.launchserver.websocket.json.SimpleResponse;

public class ExecCommandResponse extends SimpleResponse {
    public String cmd;

    @Override
    public String getType() {
        return "cmdExec";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (!client.isAuth || !client.permissions.canAdmin) {
            sendError("Access denied");
            return;
        }
        LaunchServer.server.commandHandler.eval(cmd, false);
        sendResult(new ExecCommandRequestEvent(true));
    }
}
