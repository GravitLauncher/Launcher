package ru.gravit.launchserver.websocket.json.profile;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.ProfileByUsernameRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.json.SimpleResponse;
import ru.gravit.utils.helper.LogHelper;

import java.util.UUID;

import static ru.gravit.launchserver.websocket.json.profile.ProfileByUUIDResponse.getProfile;

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
            uuid = LaunchServer.server.config.getAuthProviderPair().handler.usernameToUUID(username);
        } else uuid = client.auth.handler.usernameToUUID(username);
        sendResult(new ProfileByUsernameRequestEvent(getProfile(LaunchServer.server, uuid, username, this.client, client.auth.textureProvider)));
    }
}
