package pro.gravit.launchserver.auth.texture;

import java.io.IOException;
import java.util.UUID;

import pro.gravit.launcher.profiles.Texture;
import pro.gravit.utils.ProviderMap;

public abstract class TextureProvider implements AutoCloseable {
    public static final ProviderMap<TextureProvider> providers = new ProviderMap<>("TextureProvider");
    private static boolean registredProv = false;

    public static void registerProviders() {
        if (!registredProv) {
            providers.register("null", NullTextureProvider.class);
            providers.register("void", VoidTextureProvider.class);

            // Auth providers that doesn't do nothing :D
            providers.register("request", RequestTextureProvider.class);
            registredProv = true;
        }
    }

    @Override
    public abstract void close() throws IOException;


    public abstract Texture getCloakTexture(UUID uuid, String username, String client) throws IOException;


    public abstract Texture getSkinTexture(UUID uuid, String username, String client) throws IOException;
}
