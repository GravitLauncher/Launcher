package pro.gravit.launcher.core.api.features;

import pro.gravit.launcher.core.hasher.HashedDir;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ProfileFeatureAPI extends FeatureAPI {
    CompletableFuture<List<ClientProfile>> getProfiles();
    CompletableFuture<UpdateInfo> fetchUpdateInfo(String dirName);

    interface UpdateInfo {}

    interface ClientProfile {
        String getName();
        UUID getUUID();
        List<OptionalMod> getOptionalMods();
    }

    interface OptionalMod {
        String getName();
        String getDescription();
        String getCategory();
    }
}
