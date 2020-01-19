package pro.gravit.launchserver.auth;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.handler.AuthHandler;
import pro.gravit.launchserver.auth.hwid.HWIDHandler;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.texture.TextureProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthProviderPair {
    public AuthProvider provider;
    public AuthHandler handler;
    public TextureProvider textureProvider;
    public HWIDHandler hwid;
    public Map<String, String> links;
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
        if(links != null) link(srv);
        if(provider == null) throw new NullPointerException(String.format("Auth %s provider null", name));
        if(handler == null) throw new NullPointerException(String.format("Auth %s handler null", name));
        if(textureProvider == null) throw new NullPointerException(String.format("Auth %s textureProvider null", name));
        if(hwid == null) throw new NullPointerException(String.format("Auth %s hwid null", name));
        provider.init(srv);
        handler.init(srv);
        hwid.init();
    }
    public void link(LaunchServer srv)
    {
        links.forEach((k,v) -> {
            AuthProviderPair pair = srv.config.getAuthProviderPair(v);
            if(pair == null)
            {
                throw new NullPointerException(String.format("Auth %s link failed. Pair %s not found", name, v));
            }
            if("provider".equals(k))
            {
                if(pair.provider == null) throw new NullPointerException(String.format("Auth %s link failed. %s.provider is null", name, v));
                provider = pair.provider;
            }
            else if("handler".equals(k))
            {
                if(pair.handler == null) throw new NullPointerException(String.format("Auth %s link failed. %s.handler is null", name, v));
                handler = pair.handler;
            }
            else if("textureProvider".equals(k))
            {
                if(pair.textureProvider == null) throw new NullPointerException(String.format("Auth %s link failed. %s.textureProvider is null", name, v));
                textureProvider = pair.textureProvider;
            }
            else if("hwid".equals(k))
            {
                if(pair.hwid == null) throw new NullPointerException(String.format("Auth %s link failed. %s.hwid is null", name, v));
                hwid = pair.hwid;
            }
        });
    }

    public void close() throws IOException {
        provider.close();
        handler.close();
        textureProvider.close();
    }
}
