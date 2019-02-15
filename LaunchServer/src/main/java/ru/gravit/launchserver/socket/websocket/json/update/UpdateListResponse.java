package ru.gravit.launchserver.socket.websocket.json.update;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launcher.events.request.UpdateListRequestEvent;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;

import java.util.HashSet;
import java.util.Map;

public class UpdateListResponse implements JsonResponseInterface {

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
        HashSet<String> set = new HashSet<>();
        for(Map.Entry<String, SignedObjectHolder<HashedDir>> entry :  LaunchServer.server.updatesDirMap.entrySet())
            set.add(entry.getKey());
        service.sendObject(ctx, new UpdateListRequestEvent(set));
    }

}
