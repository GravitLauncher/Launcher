package pro.gravit.launchserver.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.websocket.json.SimpleResponse;
import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;

import java.util.ArrayList;
import java.util.List;

public class GetAvailabilityAuthResponse extends SimpleResponse {
    @Override
    public String getType() {
        return "getAvailabilityAuth";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        List<GetAvailabilityAuthRequestEvent.AuthAvailability> list = new ArrayList<>();
        for (AuthProviderPair pair : LaunchServer.server.config.auth) {
            list.add(new GetAvailabilityAuthRequestEvent.AuthAvailability(pair.name, pair.displayName));
        }
        sendResult(new GetAvailabilityAuthRequestEvent(list));
    }
}
