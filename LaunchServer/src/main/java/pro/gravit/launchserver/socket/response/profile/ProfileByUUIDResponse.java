package pro.gravit.launchserver.socket.response.profile;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.ProfileByUUIDRequestEvent;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import java.util.UUID;

public class ProfileByUUIDResponse extends SimpleResponse {
    public UUID uuid;
    public String client;

    @Override
    public String getType() {
        return "profileByUUID";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        AuthProviderPair pair;
        if (client.auth == null) {
            pair = server.config.getAuthProviderPair();
        } else {
            pair = client.auth;
        }
        if (pair == null) {
            sendError("ProfileByUUIDResponse: AuthProviderPair is null");
            return;
        }
        User user = pair.core.getUserByUUID(uuid);
        if (user == null) {
            sendError("User not found");
            return;
        }
        sendResult(new ProfileByUUIDRequestEvent(server.authManager.getPlayerProfile(pair, uuid)));
    }
}
