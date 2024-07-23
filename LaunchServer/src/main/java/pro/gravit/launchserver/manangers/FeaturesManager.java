package pro.gravit.launchserver.manangers;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.Version;

import java.util.HashMap;
import java.util.Map;

public class FeaturesManager {
    private final Map<String, String> map;

    public FeaturesManager(LaunchServer server) {
        map = new HashMap<>();
        addFeatureInfo("version", Version.getVersion().getVersionString());
        addFeatureInfo("projectName", server.config.projectName);
    }

    public Map<String, String> getMap() {
        return map;
    }

    public String getFeatureInfo(String name) {
        return map.get(name);
    }

    public void addFeatureInfo(String name, String featureInfo) {
        map.put(name, featureInfo);
    }

    public String removeFeatureInfo(String name) {
        return map.remove(name);
    }
}
