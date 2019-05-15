package ru.gravit.launchserver.websocket.json.update;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launcher.events.request.UpdateRequestEvent;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.json.SimpleResponse;

public class UpdateResponse extends SimpleResponse {
    public String dirName;

    @Override
    public String getType() {
        return "update";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (!client.isAuth || client.type != Client.Type.USER || client.profile == null) {
            sendError("Access denied");
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
        SignedObjectHolder<HashedDir> dir = LaunchServer.server.updatesDirMap.get(dirName);
        if (dir == null) {
            service.sendObject(ctx, new ErrorRequestEvent(String.format("Directory %s not found", dirName)));
            return;
        }
        String url = LaunchServer.server.config.netty.downloadURL.replace("%dirname%", dirName);
        boolean zip = false;
        if (server.config.netty.bindings.get(dirName) != null)
        {
            LaunchServer.NettyUpdatesBind bind = server.config.netty.bindings.get(dirName);
            url = bind.url;
            zip = bind.zip;
        }
        service.sendObject(ctx, new UpdateRequestEvent(dir.object, url, zip));
    }
}
