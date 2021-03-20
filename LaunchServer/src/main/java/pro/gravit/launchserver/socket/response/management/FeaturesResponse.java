package pro.gravit.launchserver.socket.response.management;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.FeaturesRequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class FeaturesResponse extends SimpleResponse {
    @Override
    public String getType() {
        return "features";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        sendResult(new FeaturesRequestEvent(server.featuresManager.getMap()));
    }
}
