package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportRemoteClientAccess;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import java.util.ArrayList;
import java.util.List;

public class GetAvailabilityAuthResponse extends SimpleResponse {
    @Override
    public String getType() {
        return "getAvailabilityAuth";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        List<GetAvailabilityAuthRequestEvent.AuthAvailability> list = new ArrayList<>();
        for (AuthProviderPair pair : server.config.auth.values()) {
            var rca = pair.isSupport(AuthSupportRemoteClientAccess.class);
            if (rca != null) {
                list.add(new GetAvailabilityAuthRequestEvent.AuthAvailability(pair.name, pair.displayName,
                        pair.core.getDetails(client), rca.getClientApiUrl(), rca.getClientApiFeatures()));
            } else {
                list.add(new GetAvailabilityAuthRequestEvent.AuthAvailability(pair.name, pair.displayName,
                        pair.core.getDetails(client)));
            }
        }
        sendResult(new GetAvailabilityAuthRequestEvent(list));
    }
}
