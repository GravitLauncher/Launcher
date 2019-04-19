package ru.gravit.launchserver.auth.texture;

import ru.gravit.launcher.profiles.Texture;
import ru.gravit.utils.ProviderMap;

import java.io.IOException;
import java.util.UUID;

public abstract class TextureProvider implements AutoCloseable {
    public static ProviderMap<TextureProvider> providers = new ProviderMap<>();
    private static boolean registredProv = false;

    public static void registerProviders() {
        if (!registredProv) {
            providers.registerProvider("null", NullTextureProvider.class);
            providers.registerProvider("void", VoidTextureProvider.class);

            // Auth providers that doesn't do nothing :D
            providers.registerProvider("request", RequestTextureProvider.class);
            registredProv = true;
        }
    }

    @Override
    public abstract void close() throws IOException;


    public abstract Texture getCloakTexture(UUID uuid, String username, String client) throws IOException;


    public abstract Texture getSkinTexture(UUID uuid, String username, String client) throws IOException;
}
