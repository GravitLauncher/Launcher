package ru.gravit.launchserver.auth;

import ru.gravit.launchserver.auth.handler.AuthHandler;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.texture.TextureProvider;

import java.io.IOException;

public class AuthProviderPair {
    public AuthProvider provider;
    public AuthHandler handler;
    public TextureProvider textureProvider;
    public String name;
    public boolean isDefault = true;

    public AuthProviderPair(AuthProvider provider, AuthHandler handler, TextureProvider textureProvider, String name) {
        this.provider = provider;
        this.handler = handler;
        this.textureProvider = textureProvider;
        this.name = name;
    }

    public void init()
    {
        provider.init();
        handler.init();
    }

    public void close() throws IOException {
        provider.close();
        handler.close();
        textureProvider.close();
    }
}
