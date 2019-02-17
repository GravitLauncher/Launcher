package ru.gravit.launchserver.socket.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launcher.events.request.SetProfileRequestEvent;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;

import java.util.Collection;

public class SetProfileResponse implements JsonResponseInterface {
    public String client;
    @Override
    public String getType() {
        return "setProfile";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        if(!client.isAuth)
        {
            service.sendObject(ctx, new ErrorRequestEvent("Access denied"));
            return;
        }
        Collection<ClientProfile> profiles = LaunchServer.server.getProfiles();
        for (ClientProfile p : profiles) {
            if (p.getTitle().equals(this.client)) {
                if (!p.isWhitelistContains(client.username)) {
                    service.sendObject(ctx, new ErrorRequestEvent(LaunchServer.server.config.whitelistRejectString));
                    return;
                }
                client.profile = p;
                service.sendObject(ctx, new SetProfileRequestEvent(p));
                return;
            }
        }
        service.sendObject(ctx, new ErrorRequestEvent("Profile not found"));
    }
}
