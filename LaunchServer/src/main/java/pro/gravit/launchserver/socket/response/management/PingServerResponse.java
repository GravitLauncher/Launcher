package pro.gravit.launchserver.socket.response.management;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.PingServerRequestEvent;
import pro.gravit.launcher.request.management.PingServerReportRequest;
import pro.gravit.launchserver.auth.protect.interfaces.ProfilesProtectHandler;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PingServerResponse extends SimpleResponse {
    public List<String> serverNames; //May be null

    @Override
    public String getType() {
        return "pingServer";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        Map<String, PingServerReportRequest.PingServerReport> map = new HashMap<>();
        if (serverNames == null) {
            server.pingServerManager.map.forEach((name, entity) -> {
                if (server.config.protectHandler instanceof ProfilesProtectHandler) {
                    if (!((ProfilesProtectHandler) server.config.protectHandler).canGetProfile(entity.profile, client)) {
                        return;
                    }
                }
                if (!entity.isExpired()) {
                    map.put(name, entity.lastReport);
                }
            });
        } else {
            sendError("Not implemented");
            return;
        }
        sendResult(new PingServerRequestEvent(map));
    }
}
