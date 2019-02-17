package ru.gravit.launchserver.socket.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launcher.events.request.ProfilesRequestEvent;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;

import java.util.List;

public class ProfilesResponse  implements JsonResponseInterface {
    @Override
    public String getType() {
        return "profiles";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        if(!client.checkSign)
        {
            service.sendObject(ctx, new ErrorRequestEvent("Access denied"));
            return;
        }
        service.sendObject(ctx, new ProfilesRequestEvent((List<ClientProfile>) LaunchServer.server.getProfiles()));
    }
}
