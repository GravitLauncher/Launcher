package pro.gravit.launchserver.auth.texture;

import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.profiles.Texture;
import pro.gravit.launchserver.HttpRequester;
import pro.gravit.utils.helper.SecurityHelper;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

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
        try {
            return fetchWithRetry(textureUrl, username, 0)
                    .orTimeout(10, TimeUnit.SECONDS)
                    .join();
        } catch (CompletionException e) {
            logger.error("JsonTextureProvider", e.getCause());
            return new HashMap<>();
        } catch (Exception e) {
            logger.error("JsonTextureProvider", e);
            return new HashMap<>();
        }
    }

    private CompletableFuture<Map<String, Texture>> fetchWithRetry(String textureUrl, String username, int attempt) {
        return requester.<Map<String, JsonTexture>>sendAsync(requester.get(textureUrl, bearerToken), MAP_TYPE)
                .thenCompose(result -> {
                    if (result.isSuccessful()) {
                        return CompletableFuture.completedFuture(JsonTexture.convertMap(result.result()));
                    }
                    if (result.statusCode() == 429 && attempt < 2) {
                        logger.warn("Texture API rate limited (user={}, attempt={}/3)", username, attempt + 1);
                        Executor delayed = CompletableFuture.delayedExecutor(
                                1000L * (attempt + 1), TimeUnit.MILLISECONDS);
                        return CompletableFuture.supplyAsync(() -> null, delayed)
                                .thenCompose(ignored -> fetchWithRetry(textureUrl, username, attempt + 1));
                    }
                    logger.warn("Texture API request failed (user={}, status={}, error={}, url={})",
                            username, result.statusCode(), result.error(), textureUrl);
                    return CompletableFuture.completedFuture(new HashMap<>());
                });
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
