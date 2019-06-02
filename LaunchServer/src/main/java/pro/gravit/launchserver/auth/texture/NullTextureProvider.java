package pro.gravit.launchserver.auth.texture;

import pro.gravit.launcher.profiles.Texture;
import pro.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public final class NullTextureProvider extends TextureProvider {
    private volatile TextureProvider provider;

    @Override
    public void close() throws IOException {
        TextureProvider provider = this.provider;
        if (provider != null)
            provider.close();
    }

    @Override
    public Texture getCloakTexture(UUID uuid, String username, String client) throws IOException {
        return getProvider().getCloakTexture(uuid, username, client);
    }

    private TextureProvider getProvider() {
        return VerifyHelper.verify(provider, Objects::nonNull, "Backend texture provider wasn't set");
    }

    @Override
    public Texture getSkinTexture(UUID uuid, String username, String client) throws IOException {
        return getProvider().getSkinTexture(uuid, username, client);
    }


    public void setBackend(TextureProvider provider) {
        this.provider = provider;
    }
}
