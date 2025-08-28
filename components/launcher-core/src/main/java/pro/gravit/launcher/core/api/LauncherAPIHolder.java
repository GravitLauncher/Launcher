package pro.gravit.launcher.core.api;

import pro.gravit.launcher.core.api.features.AuthFeatureAPI;
import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.api.features.UserFeatureAPI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class LauncherAPIHolder {
    private static volatile CoreFeatureAPI coreAPI;
    private static volatile LauncherAPI api;
    private static volatile Function<String, LauncherAPI> createApiFactory;
    private static final Map<String, LauncherAPI> map = new ConcurrentHashMap<>();

    public static void setCoreAPI(CoreFeatureAPI coreAPI) {
        LauncherAPIHolder.coreAPI = coreAPI;
    }

    public static void setApi(LauncherAPI api) {
        LauncherAPIHolder.api = api;
    }

    public static void setCreateApiFactory(Function<String, LauncherAPI> createApiFactory) {
        LauncherAPIHolder.createApiFactory = createApiFactory;
    }

    public static void changeAuthId(String authId) {
        LauncherAPIHolder.api = map.computeIfAbsent(authId, createApiFactory);
    }

    public static LauncherAPI get() {
        return api;
    }

    public static LauncherAPI get(String authId) {
        return map.computeIfAbsent(authId, createApiFactory);
    }

    public static CoreFeatureAPI core() {
        return coreAPI;
    }

    public static AuthFeatureAPI auth() {
        if(api == null) {
            throw new UnsupportedOperationException();
        }
        return api.auth();
    }

    public static UserFeatureAPI user() {
        if(api == null) {
            throw new UnsupportedOperationException();
        }
        return api.user();
    }

    public static ProfileFeatureAPI profile() {
        if(api == null) {
            throw new UnsupportedOperationException();
        }
        return api.profile();
    }

    public static void set(LauncherAPI api) {
        LauncherAPIHolder.api = api;
    }
}
