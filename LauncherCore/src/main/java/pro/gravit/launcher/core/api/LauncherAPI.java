package pro.gravit.launcher.core.api;

import pro.gravit.launcher.core.api.features.AuthFeatureAPI;
import pro.gravit.launcher.core.api.features.FeatureAPI;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.api.features.UserFeatureAPI;

import java.util.HashMap;
import java.util.Map;

public class LauncherAPI {
    private final Map<Class<? extends FeatureAPI>, FeatureAPI> map;

    public LauncherAPI(Map<Class<? extends FeatureAPI>, FeatureAPI> map) {
        this.map = new HashMap<>(map);
    }

    public AuthFeatureAPI auth() {
        return get(AuthFeatureAPI.class);
    }

    public UserFeatureAPI user() {
        return get(UserFeatureAPI.class);
    }

    public ProfileFeatureAPI profile() {
        return get(ProfileFeatureAPI.class);
    }

    @SuppressWarnings("unchecked")
    public<T extends FeatureAPI> T get(Class<T> clazz) {
        return (T) map.get(clazz);
    }
}
