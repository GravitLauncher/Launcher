package pro.gravit.launchserver.socket.response.management;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.request.PingServerReportRequestEvent;
import pro.gravit.launcher.request.management.PingServerReportRequest;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class PingServerReportResponse extends SimpleResponse {
    public PingServerReportRequest.PingServerReport data;
    public String name;

    @Override
    public String getType() {
        return "pingServerReport";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (!client.isAuth || client.permissions == null || !client.permissions.isPermission(ClientPermissions.PermissionConsts.MANAGEMENT)) {
            sendError("Access denied");
            return;
        }
        server.pingServerManager.updateServer(name, data);
        sendResult(new PingServerReportRequestEvent());
    }
}
