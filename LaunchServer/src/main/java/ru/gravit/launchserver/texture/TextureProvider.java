package ru.gravit.launchserver.texture;

import ru.gravit.launcher.profiles.Texture;
import ru.gravit.launcher.serialize.config.ConfigObject;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TextureProvider extends ConfigObject implements AutoCloseable {
    private static final Map<String, Adapter<TextureProvider>> TEXTURE_PROVIDERS = new ConcurrentHashMap<>(2);
    private static boolean registredProv = false;


    public static TextureProvider newProvider(String name, BlockConfigEntry block) {
        VerifyHelper.verifyIDName(name);
        Adapter<TextureProvider> authHandlerAdapter = VerifyHelper.getMapValue(TEXTURE_PROVIDERS, name,
                String.format("Unknown texture provider: '%s'", name));
        return authHandlerAdapter.convert(block);
    }


    public static void registerProvider(String name, Adapter<TextureProvider> adapter) {
        VerifyHelper.putIfAbsent(TEXTURE_PROVIDERS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("Texture provider has been already registered: '%s'", name));
    }

    public static void registerProviders() {
        if (!registredProv) {
            registerProvider("null", NullTextureProvider::new);
            registerProvider("void", VoidTextureProvider::new);

            // Auth providers that doesn't do nothing :D
            registerProvider("request", RequestTextureProvider::new);
            registredProv = true;
        }
    }


    protected TextureProvider(BlockConfigEntry block) {
        super(block);
    }

    @Override
    public abstract void close() throws IOException;


    public abstract Texture getCloakTexture(UUID uuid, String username, String client) throws IOException;


    public abstract Texture getSkinTexture(UUID uuid, String username, String client) throws IOException;
}
