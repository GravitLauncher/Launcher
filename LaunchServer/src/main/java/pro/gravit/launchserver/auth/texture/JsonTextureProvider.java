package pro.gravit.launchserver.auth.texture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.HTTPRequest;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.profiles.Texture;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public class JsonTextureProvider extends TextureProvider {
    public String url;
    private transient final Logger logger = LogManager.getLogger();

    @Override
    public void close() throws IOException {
        //None
    }

    @Override
    public Texture getCloakTexture(UUID uuid, String username, String client) throws IOException {
        logger.warn("Ineffective get cloak texture for {}", username);
        return getTextures(uuid, username, client).cloak;
    }

    @Override
    public Texture getSkinTexture(UUID uuid, String username, String client) throws IOException {
        logger.warn("Ineffective get skin texture for {}", username);
        return getTextures(uuid, username, client).skin;
    }

    @Override
    public SkinAndCloakTextures getTextures(UUID uuid, String username, String client) {
        try {
            var result = HTTPRequest.jsonRequest(null, "GET", new URL(RequestTextureProvider.getTextureURL(url, uuid, username, client)));
            return Launcher.gsonManager.gson.fromJson(result, SkinAndCloakTextures.class);
        } catch (IOException e) {
            logger.error("JsonTextureProvider", e);
            return new SkinAndCloakTextures(null, null);
        }
    }
}
