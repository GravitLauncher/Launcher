package ru.gravit.launchserver.auth;

import ru.gravit.launchserver.auth.handler.AuthHandler;
import ru.gravit.launchserver.auth.provider.AuthProvider;

import java.io.IOException;

public class AuthProviderPair {
    public AuthProvider provider;
    public AuthHandler handler;
    public String name;
    public boolean isDefault;

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
