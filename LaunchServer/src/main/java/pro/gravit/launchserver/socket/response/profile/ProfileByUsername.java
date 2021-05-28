package pro.gravit.launchserver.socket.response.profile;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.ProfileByUsernameRequestEvent;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class ProfileByUsername extends SimpleResponse {
    String username;
    String client;

    @Override
    public String getType() {
        return "profileByUsername";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        AuthProviderPair pair = client.auth;
        if (pair == null) pair = server.config.getAuthProviderPair();
        PlayerProfile profile = server.authManager.getPlayerProfile(pair, username);
        if (profile == null) {
            sendError("User not found");
            return;
        }
        sendResult(new ProfileByUsernameRequestEvent(profile));
    }
}
