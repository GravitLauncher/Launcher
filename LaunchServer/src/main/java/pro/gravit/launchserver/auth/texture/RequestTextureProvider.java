package pro.gravit.launchserver.auth.texture;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.profiles.Texture;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

public final class RequestTextureProvider extends TextureProvider {
    // Instance
    private String skinURL;
    private String cloakURL;

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

    private static String getTextureURL(String url, UUID uuid, String username, String client) {
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
        return getTexture(getTextureURL(cloakURL, uuid, username, client), true);
    }

    @Override
    public Texture getSkinTexture(UUID uuid, String username, String client) throws IOException {
        return getTexture(getTextureURL(skinURL, uuid, username, client), false);
    }
}
