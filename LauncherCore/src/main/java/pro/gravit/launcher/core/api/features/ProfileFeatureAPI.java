package pro.gravit.launcher.core.api.features;

import pro.gravit.launcher.core.hasher.HashedDir;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ProfileFeatureAPI extends FeatureAPI {
    CompletableFuture<List<ClientProfile>> getProfiles();
    CompletableFuture<Void> changeCurrentProfile(ClientProfile profile);
    CompletableFuture<UpdateInfo> fetchUpdateInfo(String dirName);

    interface UpdateInfo {
        HashedDir getHashedDir();
        String getUrl();
    }

    interface ClientProfile {
        String getName();
        UUID getUUID();
        String getDescription();
        List<OptionalMod> getOptionalMods();
        String getProperty(String name);
        Map<String, String> getProperties();
        ServerInfo getServer();

        interface ServerInfo {
            String getAddress();
            int getPort();
        }
    }

    interface OptionalMod {
        String getName();
        String getDescription();
        String getCategory();
        boolean isVisible();
        Set<OptionalMod> getDependencies();
    }
}
