package ru.gravit.launchserver.socket.websocket.json.update;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launcher.events.request.UpdateRequestEvent;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;

public class UpdateResponse implements JsonResponseInterface {
    public String dirName;

    @Override
    public String getType() {
        return "update";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        if (!client.isAuth || client.type != Client.Type.USER || client.profile == null) {
            service.sendObject(ctx, new ErrorRequestEvent("Access denied"));
            return;
        }
        if (!client.permissions.canAdmin) {
            for (ClientProfile p : LaunchServer.server.getProfiles()) {
                if (!client.profile.getTitle().equals(p.getTitle())) continue;
                if (!p.isWhitelistContains(client.username)) {
                    service.sendObject(ctx, new ErrorRequestEvent("You don't download this folder"));
                    return;
                }
            }
        }
        service.sendObject(ctx, new UpdateRequestEvent(LaunchServer.server.updatesDirMap.get(dirName).object, LaunchServer.server.config.netty.downloadURL.replace("%dirname%",dirName)));
    }
}
