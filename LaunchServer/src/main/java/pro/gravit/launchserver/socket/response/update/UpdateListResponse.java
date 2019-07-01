package pro.gravit.launchserver.socket.response.update;

import java.util.HashSet;
import java.util.Map;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.UpdateListRequestEvent;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.serialize.signed.SignedObjectHolder;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

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
        for (Map.Entry<String, SignedObjectHolder<HashedDir>> entry : server.updatesDirMap.entrySet())
            set.add(entry.getKey());
        sendResult(new UpdateListRequestEvent(set));
    }

}
