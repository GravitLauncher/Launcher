package pro.gravit.launchserver.auth.texture;

import java.util.UUID;

import pro.gravit.launcher.profiles.Texture;

public final class VoidTextureProvider extends TextureProvider {

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
