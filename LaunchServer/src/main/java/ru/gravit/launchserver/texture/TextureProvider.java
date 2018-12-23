package ru.gravit.launchserver.texture;

import ru.gravit.launcher.profiles.Texture;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TextureProvider implements AutoCloseable {
    private static final Map<String, Class> TEXTURE_PROVIDERS = new ConcurrentHashMap<>(2);
    private static boolean registredProv = false;


    public static void registerProvider(String name, Class adapter) {
        VerifyHelper.putIfAbsent(TEXTURE_PROVIDERS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("Texture provider has been already registered: '%s'", name));
    }

    public static void registerProviders() {
        if (!registredProv) {
            registerProvider("null", NullTextureProvider.class);
            registerProvider("void", VoidTextureProvider.class);

            // Auth providers that doesn't do nothing :D
            registerProvider("request", RequestTextureProvider.class);
            registredProv = true;
        }
    }

    @Override
    public abstract void close() throws IOException;


    public abstract Texture getCloakTexture(UUID uuid, String username, String client) throws IOException;


    public abstract Texture getSkinTexture(UUID uuid, String username, String client) throws IOException;
}
