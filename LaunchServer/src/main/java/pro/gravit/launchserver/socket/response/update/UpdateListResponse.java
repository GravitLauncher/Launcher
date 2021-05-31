package pro.gravit.launchserver.socket.response.update;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.UpdateListRequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import java.util.HashSet;

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
        HashSet<String> set = server.updatesManager.getUpdatesList();
        sendResult(new UpdateListRequestEvent(set));
    }

}
