package ru.gravit.launchserver.socket.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;

import java.util.Collection;

public class SetProfileResponse implements JsonResponseInterface {
    @Override
    public String getType() {
        return "setProfile";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        if(!client.isAuth)
        {
            service.sendObject(ctx, new WebSocketService.ErrorResult("Access denied"));
            return;
        }
        Collection<ClientProfile> profiles = LaunchServer.server.getProfiles();
        for (ClientProfile p : profiles) {
            if (p.getTitle().equals(client)) {
                if (!p.isWhitelistContains(client.username)) {
                    service.sendObject(ctx, new WebSocketService.ErrorResult(LaunchServer.server.config.whitelistRejectString));
                    return;
                }
                client.profile = p;
                service.sendObject(ctx, new WebSocketService.SuccessResult(getType()));
                break;
            }
        }
        service.sendObject(ctx, new WebSocketService.ErrorResult("Profile not found"));
    }
}
