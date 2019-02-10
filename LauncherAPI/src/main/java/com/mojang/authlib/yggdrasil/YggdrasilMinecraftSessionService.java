package com.mojang.authlib.yggdrasil;

import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.BaseMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.auth.CheckServerRequest;
import ru.gravit.launcher.request.auth.JoinServerRequest;
import ru.gravit.launcher.request.uuid.ProfileByUUIDRequest;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.net.InetAddress;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class YggdrasilMinecraftSessionService extends BaseMinecraftSessionService {
    public static final JsonParser JSON_PARSER = new JsonParser();
    public static final boolean NO_TEXTURES = Boolean.parseBoolean("launcher.com.mojang.authlib.noTextures");

    public static void fillTextureProperties(GameProfile profile, PlayerProfile pp) {
        LogHelper.debug("fillTextureProperties, Username: '%s'", profile.getName());
        if (NO_TEXTURES)
            return;

        // Fill textures map
        PropertyMap properties = profile.getProperties();
        if (pp.skin != null) {
            properties.put(Launcher.SKIN_URL_PROPERTY, new Property(Launcher.SKIN_URL_PROPERTY, pp.skin.url, ""));
            properties.put(Launcher.SKIN_DIGEST_PROPERTY, new Property(Launcher.SKIN_DIGEST_PROPERTY, SecurityHelper.toHex(pp.skin.digest), ""));
            LogHelper.debug("fillTextureProperties, Has skin texture for username '%s'", profile.getName());
        }
        if (pp.cloak != null) {
            properties.put(Launcher.CLOAK_URL_PROPERTY, new Property(Launcher.CLOAK_URL_PROPERTY, pp.cloak.url, ""));
            properties.put(Launcher.CLOAK_DIGEST_PROPERTY, new Property(Launcher.CLOAK_DIGEST_PROPERTY, SecurityHelper.toHex(pp.cloak.digest), ""));
            LogHelper.debug("fillTextureProperties, Has cloak texture for username '%s'", profile.getName());
        }
    }

    private static void getTexturesMojang(Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures, String texturesBase64, GameProfile profile) {
        // Decode textures payload
        JsonObject texturesJSON;
        try {
            byte[] decoded = Base64.getDecoder().decode(texturesBase64);
            texturesJSON = JSON_PARSER.parse(new String(decoded, IOHelper.UNICODE_CHARSET)).getAsJsonObject().getAsJsonObject("textures");
        } catch (Exception ignored) {
            LogHelper.error("Could not decode textures payload, Username: '%s', UUID: '%s'", profile.getName(), profile.getUUID());
            return;
        }

        // Fetch textures from textures JSON
        for (MinecraftProfileTexture.Type type : MinecraftProfileTexture.PROFILE_TEXTURE_TYPES) {
            if (textures.containsKey(type))
                continue; // Overriden by launcher

            // Get texture from JSON
            JsonElement textureJSON = texturesJSON.get(type.name());
            if (textureJSON != null && textureJSON.isJsonObject()) {
                JsonElement urlValue = textureJSON.getAsJsonObject().get("url");
                if (urlValue.isJsonPrimitive())
                    textures.put(type, new MinecraftProfileTexture(urlValue.getAsString()));
            }
        }
    }

    public static GameProfile toGameProfile(PlayerProfile pp) {
        GameProfile profile = new GameProfile(pp.uuid, pp.username);
        fillTextureProperties(profile, pp);
        return profile;
    }

    public YggdrasilMinecraftSessionService(AuthenticationService service) {
        super(service);
        LogHelper.debug("Patched MinecraftSessionService created");
    }

    @Override
    public GameProfile fillProfileProperties(GameProfile profile, boolean requireSecure) {
        // Verify has UUID
        UUID uuid = profile.getUUID();
        LogHelper.debug("fillProfileProperties, UUID: %s", uuid);
        if (uuid == null)
            return profile;

        // Make profile request
        PlayerProfile pp;
        try {
            pp = new ProfileByUUIDRequest(uuid).request().playerProfile;
        } catch (Exception e) {
            LogHelper.debug("Couldn't fetch profile properties for '%s': %s", profile, e);
            return profile;
        }

        // Verify is found
        if (pp == null) {
            LogHelper.debug("Couldn't fetch profile properties for '%s' as the profile does not exist", profile);
            return profile;
        }

        // Create new game profile from player profile
        LogHelper.debug("Successfully fetched profile properties for '%s'", profile);
        fillTextureProperties(profile, pp);
        return toGameProfile(pp);
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
        LogHelper.debug("getTextures, Username: '%s', UUID: '%s'", profile.getName(), profile.getUUID());
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = new EnumMap<>(MinecraftProfileTexture.Type.class);

        // Add textures
        if (!NO_TEXTURES) {
            // Add skin URL to textures map
            Property skinURL = Iterables.getFirst(profile.getProperties().get(Launcher.SKIN_URL_PROPERTY), null);
            Property skinDigest = Iterables.getFirst(profile.getProperties().get(Launcher.SKIN_DIGEST_PROPERTY), null);
            if (skinURL != null && skinDigest != null)
                textures.put(MinecraftProfileTexture.Type.SKIN, new MinecraftProfileTexture(skinURL.getValue(), skinDigest.getValue()));

            // Add cloak URL to textures map
            Property cloakURL = Iterables.getFirst(profile.getProperties().get(Launcher.CLOAK_URL_PROPERTY), null);
            Property cloakDigest = Iterables.getFirst(profile.getProperties().get(Launcher.CLOAK_DIGEST_PROPERTY), null);
            if (cloakURL != null && cloakDigest != null)
                textures.put(MinecraftProfileTexture.Type.CAPE, new MinecraftProfileTexture(cloakURL.getValue(), cloakDigest.getValue()));

            // Try to find missing textures in textures payload (now always true because launcher is not passing elytra skins)
            if (textures.size() != MinecraftProfileTexture.PROFILE_TEXTURE_COUNT) {
                Property texturesMojang = Iterables.getFirst(profile.getProperties().get("textures"), null);
                if (texturesMojang != null)
                    getTexturesMojang(textures, texturesMojang.getValue(), profile);
            }
        }

        // Return filled textures
        return textures;
    }

    @Override
    public GameProfile hasJoinedServer(GameProfile profile, String serverID) throws AuthenticationUnavailableException {
        String username = profile.getName();
        LogHelper.debug("checkServer, Username: '%s', Server ID: %s", username, serverID);

        // Make checkServer request
        PlayerProfile pp;
        try {
            pp = new CheckServerRequest(username, serverID).request().playerProfile;
        } catch (Exception e) {
            LogHelper.error(e);
            throw new AuthenticationUnavailableException(e);
        }

        // Return profile if found
        return pp == null ? null : toGameProfile(pp);
    }

    @Override
    public GameProfile hasJoinedServer(GameProfile profile, String serverID, InetAddress address) throws AuthenticationUnavailableException {
        return hasJoinedServer(profile, serverID);
    }

    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService) super.getAuthenticationService();
    }

    @Override
    public void joinServer(GameProfile profile, String accessToken, String serverID) throws AuthenticationException {

        // Join server
        String username = profile.getName();
        LogHelper.debug("joinServer, Username: '%s', Access token: %s, Server ID: %s", username, accessToken, serverID);

        // Make joinServer request
        boolean success;
        try {
            success = new JoinServerRequest(username, accessToken, serverID).request().allow;
        } catch (Exception e) {
            throw new AuthenticationUnavailableException(e);
        }

        // Verify is success
        if (!success)
            throw new AuthenticationException("Bad Login (Clientside)");
    }
}
