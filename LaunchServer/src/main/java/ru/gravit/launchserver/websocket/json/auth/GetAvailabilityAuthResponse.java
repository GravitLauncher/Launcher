package ru.gravit.launchserver.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthProviderPair;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.WebSocketService;
import ru.gravit.launchserver.websocket.json.JsonResponseInterface;

import java.util.ArrayList;
import java.util.List;

public class GetAvailabilityAuthResponse implements JsonResponseInterface {
    @Override
    public String getType() {
        return "getAvailabilityAuth";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        List<GetAvailabilityAuthRequestEvent.AuthAvailability> list = new ArrayList<>();
        for(AuthProviderPair pair : LaunchServer.server.config.auth)
        {
            list.add(new GetAvailabilityAuthRequestEvent.AuthAvailability(pair.name, pair.displayName));
        }
        service.sendObject(ctx, new GetAvailabilityAuthRequestEvent(list));
    }
}
