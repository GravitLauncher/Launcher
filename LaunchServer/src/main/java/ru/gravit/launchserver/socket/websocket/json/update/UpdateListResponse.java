package ru.gravit.launchserver.socket.websocket.json.update;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;

public class UpdateListResponse implements JsonResponseInterface {
    public String dir;
    @Override
    public String getType() {
        return "updateList";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        if(!client.isAuth) {
            service.sendObject(ctx,new WebSocketService.ErrorResult("Access denied"));
            return;
        }
        HashedDir hdir = LaunchServer.server.updatesDirMap.get(dir).object;
        service.sendObject(ctx,new Result(hdir));
    }
    class Result
    {
        public final String type;
        public final String requesttype;
        public final HashedDir dir;

        Result(HashedDir dir) {
            this.dir = dir;
            type = "success";
            requesttype = "updateList";
        }
    }
}
