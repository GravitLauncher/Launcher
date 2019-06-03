package pro.gravit.launchserver.websocket.json.admin;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.ExecCommandRequestEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.websocket.json.SimpleResponse;

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
