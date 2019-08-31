package pro.gravit.launchserver.socket.response.update;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.ErrorRequestEvent;
import pro.gravit.launcher.events.request.UpdateRequestEvent;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.utils.helper.IOHelper;

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
            for (ClientProfile p : server.getProfiles()) {
                if (!client.profile.getTitle().equals(p.getTitle())) continue;
                if (!p.isWhitelistContains(client.username)) {
                    service.sendObject(ctx, new ErrorRequestEvent("You don't download this folder"));
                    return;
                }
            }
        }
        HashedDir dir = server.updatesDirMap.get(dirName);
        if (dir == null) {
            service.sendObject(ctx, new ErrorRequestEvent(String.format("Directory %s not found", dirName)));
            return;
        }
        String url = server.config.netty.downloadURL.replace("%dirname%", IOHelper.urlEncode(dirName));
        boolean zip = false;
        if (server.config.netty.bindings.get(dirName) != null) {
            LaunchServerConfig.NettyUpdatesBind bind = server.config.netty.bindings.get(dirName);
            url = bind.url;
            zip = bind.zip;
        }
        service.sendObject(ctx, new UpdateRequestEvent(dir, url, zip));
    }
}
