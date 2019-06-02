package pro.gravit.launchserver.auth;

import pro.gravit.launchserver.auth.handler.AuthHandler;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.texture.TextureProvider;

import java.io.IOException;

public class AuthProviderPair {
    public AuthProvider provider;
    public AuthHandler handler;
    public TextureProvider textureProvider;
    public String name;
    public String displayName;
    public boolean isDefault = true;

    public AuthProviderPair(AuthProvider provider, AuthHandler handler, TextureProvider textureProvider, String name) {
        this.provider = provider;
        this.handler = handler;
        this.textureProvider = textureProvider;
        this.name = name;
    }

    public void init() {
        provider.init();
        handler.init();
    }

    public void close() throws IOException {
        provider.close();
        handler.close();
        textureProvider.close();
    }
}
