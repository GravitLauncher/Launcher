package pro.gravit.launchserver.socket.response.profile;

import java.io.IOException;
import java.util.UUID;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.ProfileByUUIDRequestEvent;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launcher.profiles.Texture;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.texture.TextureProvider;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.utils.helper.LogHelper;

public class ProfileByUUIDResponse extends SimpleResponse {
    public UUID uuid;
    public String client;

    public static PlayerProfile getProfile(LaunchServer server, UUID uuid, String username, String client, TextureProvider textureProvider) {
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
        if (client.auth == null) {
            LogHelper.warning("Client auth is null. Using default.");
            username = server.config.getAuthProviderPair().handler.uuidToUsername(uuid);
        } else username = client.auth.handler.uuidToUsername(uuid);
        sendResult(new ProfileByUUIDRequestEvent(getProfile(server, uuid, username, this.client, client.auth.textureProvider)));
    }
}
