package pro.gravit.launchserver.auth;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.handler.AuthHandler;
import pro.gravit.launchserver.auth.hwid.HWIDHandler;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.texture.TextureProvider;

import java.io.IOException;

public class AuthProviderPair {
    public final AuthProvider provider;
    public final AuthHandler handler;
    public final TextureProvider textureProvider;
    public final HWIDHandler hwid;
    public final String name;
    public String displayName;
    public final boolean isDefault = true;

    public AuthProviderPair(AuthProvider provider, AuthHandler handler, TextureProvider textureProvider, HWIDHandler hwid, String name) {
        this.provider = provider;
        this.handler = handler;
        this.textureProvider = textureProvider;
        this.hwid = hwid;
        this.name = name;
    }

    public void init(LaunchServer srv) {
        provider.init(srv);
        handler.init(srv);
        hwid.init();
    }

    public void close() throws IOException {
        provider.close();
        handler.close();
        textureProvider.close();
    }
}
