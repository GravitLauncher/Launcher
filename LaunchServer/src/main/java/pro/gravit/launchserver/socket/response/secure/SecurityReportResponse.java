package pro.gravit.launchserver.socket.response.secure;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.SecurityReportRequestEvent;
import pro.gravit.launchserver.auth.protect.interfaces.SecureProtectHandler;
import pro.gravit.launchserver.modules.events.security.SecurityReportModuleEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class SecurityReportResponse extends SimpleResponse {
    public String reportType;
    public String smallData;
    public String largeData;
    public byte[] smallBytes;
    public byte[] largeBytes;

    @Override
    public String getType() {
        return "securityReport";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (!(server.config.protectHandler instanceof SecureProtectHandler)) {
            sendError("Method not allowed");
        }
        SecureProtectHandler secureProtectHandler = (SecureProtectHandler) server.config.protectHandler;
        SecurityReportRequestEvent event = secureProtectHandler.onSecurityReport(this, client);
        server.modulesManager.invokeEvent(new SecurityReportModuleEvent(event, this, client));
        sendResult(event);
    }
}
