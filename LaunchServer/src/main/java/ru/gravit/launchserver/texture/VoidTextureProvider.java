package ru.gravit.launchserver.texture;

import ru.gravit.launcher.profiles.Texture;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;

import java.util.UUID;

public final class VoidTextureProvider extends TextureProvider {
    public VoidTextureProvider(BlockConfigEntry block) {
        super(block);
    }

    @Override
    public void close() {
        // Do nothing
    }

    @Override
    public Texture getCloakTexture(UUID uuid, String username, String client) {
        return null; // Always nothing
    }

    @Override
    public Texture getSkinTexture(UUID uuid, String username, String client) {
        return null; // Always nothing
    }
}
