package pro.gravit.launchserver.auth.texture;

import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.profiles.Texture;
import pro.gravit.launchserver.HttpRequester;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JsonTextureProvider extends TextureProvider {
    private static final Type MAP_TYPE = new TypeToken<Map<String, JsonTexture>>() {
    }.getType();
    private transient final Logger logger = LogManager.getLogger();
    private transient final HttpRequester requester = new HttpRequester();
    public String url;
    public String bearerToken;

    @Override
    public void close() {
        //None
    }

    @Override
    public Texture getCloakTexture(UUID uuid, String username, String client) {
        logger.warn("Ineffective get cloak texture for {}", username);
        return getAssets(uuid, username, client).get("CAPE");
    }

    @Override
    public Texture getSkinTexture(UUID uuid, String username, String client) {
        logger.warn("Ineffective get skin texture for {}", username);
        return getAssets(uuid, username, client).get("SKIN");
    }

    @Override
    public Map<String, Texture> getAssets(UUID uuid, String username, String client) {
        String textureUrl = RequestTextureProvider.getTextureURL(url, uuid, username, client);
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                var result = requester.<Map<String, JsonTexture>>send(
                        requester.get(textureUrl, bearerToken), MAP_TYPE);
                if (result.isSuccessful()) {
                    return JsonTexture.convertMap(result.result());
                }
                if (result.statusCode() == 429 && attempt < 2) {
                    logger.warn("Texture API rate limited (user={}, attempt={}/3)", username, attempt + 1);
                    Thread.sleep(1000L * (attempt + 1));
                    continue;
                }
                logger.warn("Texture API request failed (user={}, status={}, error={}, url={})",
                        username, result.statusCode(), result.error(), textureUrl);
                return new HashMap<>();
            } catch (IOException e) {
                logger.error("JsonTextureProvider", e);
                return new HashMap<>();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new HashMap<>();
            }
        }
        return new HashMap<>();
    }

    public record JsonTexture(String url, String digest, Map<String, String> metadata) {
        public Texture toTexture() {
            return new Texture(url, digest == null ? null : SecurityHelper.fromHex(digest), metadata);
        }

        public static Map<String, Texture> convertMap(Map<String, JsonTexture> map) {
            if (map == null) {
                return new HashMap<>();
            }
            Map<String, Texture> res = new HashMap<>();
            for(var e : map.entrySet()) {
                res.put(e.getKey(), e.getValue().toTexture());
            }
            return res;
        }
    }
}
