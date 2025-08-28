package pro.gravit.launchserver.socket.response.update;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.base.events.request.UpdateRequestEvent;
import pro.gravit.launcher.core.hasher.HashedDir;
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
        if (dirName == null) {
            sendError("Invalid request");
            return;
        }
        if(client.profile == null) {
            sendError("Profile not setted");
            return;
        }
        HashedDir dir = null;
        if(dirName.equals(client.profile.getProfile().getDir())) {
            dir = client.profile.getClientDir();
        } else if(dirName.equals(client.profile.getProfile().getAssetDir())) {
            dir = client.profile.getAssetDir();
        } else {
            dir = server.config.profilesProvider.getUnconnectedDirectory(dirName);
        }
        if (dir == null) {
            sendError("Directory %s not found".formatted(dirName));
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
