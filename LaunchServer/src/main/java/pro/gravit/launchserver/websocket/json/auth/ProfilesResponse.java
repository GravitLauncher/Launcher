package pro.gravit.launchserver.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.ErrorRequestEvent;
import pro.gravit.launcher.events.request.ProfilesRequestEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.websocket.json.SimpleResponse;

public class ProfilesResponse extends SimpleResponse {
    @Override
    public String getType() {
        return "profiles";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (!client.checkSign && !client.isAuth) {
            service.sendObject(ctx, new ErrorRequestEvent("Access denied"));
            return;
        }
        sendResult(new ProfilesRequestEvent(LaunchServer.server.getProfiles()));
    }
}
