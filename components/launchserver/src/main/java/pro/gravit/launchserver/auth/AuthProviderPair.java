package pro.gravit.launchserver.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.mix.MixProvider;
import pro.gravit.launchserver.auth.texture.TextureProvider;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AuthProviderPair {
    private transient final Logger logger = LogManager.getLogger();
    public boolean isDefault = true;
    public AuthCoreProvider core;
    public TextureProvider textureProvider;
    public Map<String, MixProvider> mixes;
    public Map<String, String> links;
    public transient String name;
    public transient Set<String> features;
    public String displayName;
    public boolean visible = true;

    public AuthProviderPair() {
    }

    public AuthProviderPair(AuthCoreProvider core, TextureProvider textureProvider) {
        this.core = core;
        this.textureProvider = textureProvider;
    }

    public static Set<String> getFeatures(Class<?> clazz) {
        Set<String> list = new HashSet<>();
        getFeatures(clazz, list);
        return list;
    }

    public Set<String> getFeatures() {
        return features;
    }

    public static void getFeatures(Class<?> clazz, Set<String> list) {
        Feature[] features = clazz.getAnnotationsByType(Feature.class);
        for (Feature feature : features) {
            list.add(feature.value());
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            getFeatures(superClass, list);
        }
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> i : interfaces) {
            getFeatures(i, list);
        }
    }

    public <T> T isSupport(Class<T> clazz) {
        if (core == null) return null;
        T result = core.isSupport(clazz);
        if (result == null && mixes != null) {
            for(var m : mixes.values()) {
                result = m.isSupport(clazz);
                if(result != null) {
                    break;
                }
            }
        }
        return result;
    }

    public void init(LaunchServer srv, String name) {
        this.name = name;
        if (links != null) link(srv);
        core.init(srv, this);
        features = new HashSet<>();
        getFeatures(core.getClass(), features);
        if(mixes != null) {
            for(var m : mixes.values()) {
                m.init(srv, core);
                getFeatures(m.getClass(), features);
            }
        }
    }

    public void link(LaunchServer srv) {
        links.forEach((k, v) -> {
            AuthProviderPair pair = srv.config.getAuthProviderPair(v);
            if (pair == null) {
                throw new NullPointerException("Auth %s link failed. Pair %s not found".formatted(name, v));
            }
            if ("core".equals(k)) {
                if (pair.core == null)
                    throw new NullPointerException("Auth %s link failed. %s.core is null".formatted(name, v));
                core = pair.core;
            }
        });
    }

    public void close() throws IOException {
        core.close();
        if (textureProvider != null) {
            textureProvider.close();
        }
        if(mixes != null) {
            for(var m : mixes.values()) {
                m.close();
            }
        }
    }
}
