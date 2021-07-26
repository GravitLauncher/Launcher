package pro.gravit.launchserver.auth.texture;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.profiles.Texture;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public final class RequestTextureProvider extends TextureProvider {
    // Instance
    public String skinURL;
    public String cloakURL;

    public String skinLocalPath;
    public String cloakLocalPath;

    public RequestTextureProvider() {
    }

    public RequestTextureProvider(String skinURL, String cloakURL) {
        this.skinURL = skinURL;
        this.cloakURL = cloakURL;
    }

    private static Texture getTexture(String url, boolean cloak) throws IOException {
        try {
            return new Texture(url, cloak);
        } catch (FileNotFoundException ignored) {
            return null; // Simply not found
        }
    }

    private static Texture getTexture(String url, Path local, boolean cloak) throws IOException {
        try {
            return new Texture(url, local, cloak);
        } catch (FileNotFoundException ignored) {
            return null; // Simply not found
        }
    }

    public static String getTextureURL(String url, UUID uuid, String username, String client) {
        return CommonHelper.replace(url, "username", IOHelper.urlEncode(username),
                "uuid", IOHelper.urlEncode(uuid.toString()), "hash", IOHelper.urlEncode(Launcher.toHash(uuid)),
                "client", IOHelper.urlEncode(client == null ? "unknown" : client));
    }

    @Override
    public void close() {
        // Do nothing
    }

    @Override
    public Texture getCloakTexture(UUID uuid, String username, String client) throws IOException {
        String textureUrl = getTextureURL(cloakURL, uuid, username, client);
        if (cloakLocalPath == null) {
            return getTexture(textureUrl, true);
        } else {
            return getTexture(textureUrl, Paths.get(cloakLocalPath), true);
        }
    }

    @Override
    public Texture getSkinTexture(UUID uuid, String username, String client) throws IOException {
        String textureUrl = getTextureURL(skinURL, uuid, username, client);
        if (skinLocalPath == null) {
            return getTexture(textureUrl, false);
        } else {
            return getTexture(textureUrl, Paths.get(skinLocalPath), false);
        }
    }
}
