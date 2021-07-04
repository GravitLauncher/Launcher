package pro.gravit.launchserver.auth.core.interfaces.user;

import java.util.Map;

public interface UserSupportAdditionalData {
    String getProperty(String name);

    default String getPropertyUnprivileged(String name) {
        return getProperty(name);
    }

    default String getPropertyUnprivilegedSelf(String name) {
        return getProperty(name);
    }

    Map<String, String> getPropertiesMap();

    default Map<String, String> getPropertiesMapUnprivileged() {
        return getPropertiesMap();
    }

    default Map<String, String> getPropertiesMapUnprivilegedSelf() {
        return getPropertiesMapUnprivileged();
    }
}
