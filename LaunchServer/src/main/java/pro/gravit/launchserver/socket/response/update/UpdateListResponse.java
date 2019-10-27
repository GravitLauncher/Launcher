package pro.gravit.launchserver.socket.response.update;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.UpdateListRequestEvent;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import java.util.HashSet;
import java.util.Map;

public class UpdateListResponse extends SimpleResponse {

    @Override
    public String getType() {
        return "updateList";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        if (!client.isAuth) {
            sendError("Access denied");
            return;
        }
        HashSet<String> set = new HashSet<>();
        for (Map.Entry<String, HashedDir> entry : server.updatesDirMap.entrySet())
            set.add(entry.getKey());
        sendResult(new UpdateListRequestEvent(set));
    }

}
