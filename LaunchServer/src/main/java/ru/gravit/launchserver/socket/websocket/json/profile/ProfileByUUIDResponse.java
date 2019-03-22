package ru.gravit.launchserver.socket.websocket.json.profile;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.ProfileByUUIDRequestEvent;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.profiles.Texture;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;
import ru.gravit.launchserver.texture.TextureProvider;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.UUID;

public class ProfileByUUIDResponse implements JsonResponseInterface {
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
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        String username = client.auth.handler.uuidToUsername(uuid);
        service.sendObject(ctx, new ProfileByUUIDRequestEvent(getProfile(LaunchServer.server,uuid,username,this.client, client.auth.textureProvider)));
    }
}
