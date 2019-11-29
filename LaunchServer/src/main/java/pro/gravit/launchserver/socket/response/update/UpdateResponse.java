package pro.gravit.launchserver.socket.response.update;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.UpdateRequestEvent;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.IOHelper;

public class UpdateResponse extends SimpleResponse {
    public String dirName;

    @Override
    public String getType() {
        return "update";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (!client.isAuth || client.type != AuthResponse.ConnectTypes.CLIENT || client.profile == null) {
            sendError("Access denied");
            return;
        }
        if (!client.permissions.canAdmin) {
            for (ClientProfile p : server.getProfiles()) {
                if (!client.profile.getTitle().equals(p.getTitle())) continue;
                if (!p.isWhitelistContains(client.username)) {
                    sendError("You don't download this folder");
                    return;
                }
            }
        }
        HashedDir dir = server.updatesDirMap.get(dirName);
        if (dir == null) {
            sendError(String.format("Directory %s not found", dirName));
            return;
        }
        String url = server.config.netty.downloadURL.replace("%dirname%", IOHelper.urlEncode(dirName));
        boolean zip = false;
        if (server.config.netty.bindings.get(dirName) != null) {
            LaunchServerConfig.NettyUpdatesBind bind = server.config.netty.bindings.get(dirName);
            url = bind.url;
            zip = bind.zip;
        }
        sendResult(new UpdateRequestEvent(dir, url, zip));
    }
}
