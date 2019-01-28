package ru.gravit.launchserver.socket.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
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
        if(!client.isAuth)
        {
            service.sendObject(ctx, new WebSocketService.ErrorResult("Access denied"));
        }
        service.sendObject(ctx, new Result((List<ClientProfile>) LaunchServer.server.getProfiles()));
    }
    public class Result
    {
        List<ClientProfile> profiles;

        public Result(List<ClientProfile> profiles) {
            this.profiles = profiles;
        }

        String requesttype = "profilesList";
        String error;
    }
}
