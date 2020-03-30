package pro.gravit.launchserver.socket.response.admin;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.request.ExecCommandRequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class ExecCommandResponse extends SimpleResponse {
    public String cmd;

    @Override
    public String getType() {
        return "cmdExec";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (!client.isAuth || !client.permissions.isPermission(ClientPermissions.PermissionConsts.ADMIN)) {
            sendError("Access denied");
            return;
        }
        server.commandHandler.eval(cmd, false);
        sendResult(new ExecCommandRequestEvent(true));
    }
}
