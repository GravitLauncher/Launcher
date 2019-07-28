package pro.gravit.launchserver.socket.response.profile;

import java.io.IOException;
import java.util.UUID;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.ProfileByUUIDRequestEvent;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launcher.profiles.Texture;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.texture.TextureProvider;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.utils.helper.LogHelper;

public class ProfileByUUIDResponse extends SimpleResponse {
    public UUID uuid;
    public String client;

    public static PlayerProfile getProfile(UUID uuid, String username, String client, TextureProvider textureProvider) {
        // Get skin texture
        Texture skin;
        try {
            skin = textureProvider.getSkinTexture(uuid, username, client);
        } catch (IOException e) {
            LogHelper.error(new IOException(String.format("Can't get skin texture: '%s'", username), e));
            skin = null;
        }

        // Get cloak texture
        Texture cloak;
        try {
            cloak = textureProvider.getCloakTexture(uuid, username, client);
        } catch (IOException e) {
            LogHelper.error(new IOException(String.format("Can't get cloak texture: '%s'", username), e));
            cloak = null;
        }

        // Return combined profile
        return new PlayerProfile(uuid, username, skin, cloak);
    }

    @Override
    public String getType() {
        return "profileByUUID";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        String username;
        AuthProviderPair pair;
        if (client.auth == null) {
            LogHelper.warning("Client auth is null. Using default.");
            pair = server.config.getAuthProviderPair();
        } else {
            pair = client.auth;
        }
        if(pair == null)
        {
            sendError("ProfileByUUIDResponse: AuthProviderPair is null");
            return;
        }
        username = pair.handler.uuidToUsername(uuid);
        sendResult(new ProfileByUUIDRequestEvent(getProfile(uuid, username, this.client, client.auth.textureProvider)));
    }
}
