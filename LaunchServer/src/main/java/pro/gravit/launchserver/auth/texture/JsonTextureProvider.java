package pro.gravit.launchserver.auth.texture;

import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.HTTPRequest;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.profiles.Texture;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JsonTextureProvider extends TextureProvider {
    public String url;
    private transient final Logger logger = LogManager.getLogger();
    private transient static final Type MAP_TYPE = new TypeToken<Map<String, Texture>>() {}.getType();

    @Override
    public void close() throws IOException {
        //None
    }

    @Override
    public Texture getCloakTexture(UUID uuid, String username, String client) throws IOException {
        logger.warn("Ineffective get cloak texture for {}", username);
        return getAssets(uuid, username, client).get("CAPE");
    }

    @Override
    public Texture getSkinTexture(UUID uuid, String username, String client) throws IOException {
        logger.warn("Ineffective get skin texture for {}", username);
        return getAssets(uuid, username, client).get("SKIN");
    }

    @Override
    public Map<String, Texture> getAssets(UUID uuid, String username, String client) {
        try {
            var result = HTTPRequest.jsonRequest(null, "GET", new URL(RequestTextureProvider.getTextureURL(url, uuid, username, client)));

            Map<String, Texture> map = Launcher.gsonManager.gson.fromJson(result, MAP_TYPE);
            if(map == null) {
                return new HashMap<>();
            }
            if(map.get("skin") != null) { // Legacy script
                map.put("SKIN", map.get("skin"));
                map.remove("skin");
            }
            if(map.get("cloak") != null) {
                map.put("CAPE", map.get("cloak"));
                map.remove("cloak");
            }
            return map;
        } catch (IOException e) {
            logger.error("JsonTextureProvider", e);
            return new HashMap<>();
        }
    }
}
