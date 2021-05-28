package pro.gravit.launchserver.socket.response.secure;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.HardwareReportRequestEvent;
import pro.gravit.launcher.request.secure.HardwareReportRequest;
import pro.gravit.launchserver.auth.protect.interfaces.HardwareProtectHandler;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class HardwareReportResponse extends SimpleResponse {
    public HardwareReportRequest.HardwareInfo hardware;

    @Override
    public String getType() {
        return "hardwareReport";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (client.trustLevel == null || client.trustLevel.publicKey == null) {
            sendError("Invalid request");
            return;
        }
        if (server.config.protectHandler instanceof HardwareProtectHandler) {
            try {
                ((HardwareProtectHandler) server.config.protectHandler).onHardwareReport(this, client);
            } catch (SecurityException e) {
                sendError(e.getMessage());
            }
        } else {
            sendResult(new HardwareReportRequestEvent());
        }
    }
}
