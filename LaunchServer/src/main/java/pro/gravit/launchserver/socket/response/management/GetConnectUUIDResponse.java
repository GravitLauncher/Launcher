package pro.gravit.launchserver.socket.response.management;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.base.events.request.GetConnectUUIDRequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class GetConnectUUIDResponse extends SimpleResponse {
    @Override
    public String getType() {
        return "getConnectUUID";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        sendResult(new GetConnectUUIDRequestEvent(connectUUID, server.shardId));
    }
}
