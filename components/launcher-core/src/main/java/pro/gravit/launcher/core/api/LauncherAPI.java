package pro.gravit.launcher.core.api;

import pro.gravit.launcher.core.api.features.*;

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

    public RestoreTempTokensFeatureAPI restores() {
        return get(RestoreTempTokensFeatureAPI.class);
    }

    @SuppressWarnings("unchecked")
    public<T extends FeatureAPI> T get(Class<T> clazz) {
        return (T) map.get(clazz);
    }
}
