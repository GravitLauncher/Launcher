package pro.gravit.launcher.core.api.model;

import java.util.Map;

public interface Texture {
    String getUrl();
    String getHash();
    Map<String, String> getMetadata();
}
