package pro.gravit.launchserver.socket.response.profile;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.ProfileByUsernameRequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.utils.helper.LogHelper;

import java.util.UUID;

public class ProfileByUsername extends SimpleResponse {
    String username;
    String client;

    @Override
    public String getType() {
        return "profileByUsername";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        UUID uuid;
        if (client.auth == null) {
            LogHelper.warning("Client auth is null. Using default.");
            uuid = server.config.getAuthProviderPair().handler.usernameToUUID(username);
        } else uuid = client.auth.handler.usernameToUUID(username);
        sendResult(new ProfileByUsernameRequestEvent(ProfileByUUIDResponse.getProfile(uuid, username, this.client, client.auth.textureProvider)));
    }
}
