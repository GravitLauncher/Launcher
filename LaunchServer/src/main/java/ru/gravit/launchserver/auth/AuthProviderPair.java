package ru.gravit.launchserver.auth;

import ru.gravit.launchserver.auth.handler.AuthHandler;
import ru.gravit.launchserver.auth.provider.AuthProvider;

import java.io.IOException;

public class AuthProviderPair {
    public AuthProvider provider;
    public AuthHandler handler;
    public String name;
    public boolean isDefault = true;

    public AuthProviderPair(AuthProvider provider, AuthHandler handler, String name) {
        this.provider = provider;
        this.handler = handler;
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
    }
}
