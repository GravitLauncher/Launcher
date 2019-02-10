package ru.gravit.launchserver.socket.websocket.json.update;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launcher.events.request.UpdateListRequestEvent;
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
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) {
        if (!client.isAuth) {
            service.sendObject(ctx, new ErrorRequestEvent("Access denied"));
            return;
        }
        HashedDir hdir = LaunchServer.server.updatesDirMap.get(dir).object;
        service.sendObject(ctx, new UpdateListRequestEvent(hdir));
    }

}
