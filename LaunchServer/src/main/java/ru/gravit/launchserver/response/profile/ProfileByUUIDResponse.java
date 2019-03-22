package ru.gravit.launchserver.response.profile;

import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.profiles.Texture;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.texture.TextureProvider;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.UUID;

public final class ProfileByUUIDResponse extends Response {

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

    public ProfileByUUIDResponse(LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        super(server, session, input, output, ip, clientData);
    }

    @Override
    public void reply() throws IOException {
        UUID uuid = input.readUUID();
        debug("UUID: " + uuid);
        String client = input.readString(SerializeLimits.MAX_CLIENT);
        // Verify has such profile
        String username = clientData.auth.handler.uuidToUsername(uuid);
        if (username == null) {
            output.writeBoolean(false);
            return;
        }

        // Write profile
        output.writeBoolean(true);
        getProfile(server, uuid, username, client, clientData.auth.textureProvider).write(output);
    }
}
