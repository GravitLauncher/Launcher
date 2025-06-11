package pro.gravit.launcher.core.api.model;

import java.util.Map;
import java.util.UUID;

public interface User {
    String getUsername();
    UUID getUUID();
    Map<String, Texture> getAssets();
    Map<String, String> getProperties();
}
