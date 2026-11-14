package pro.gravit.launcher.runtime.backend;

import org.slf4j.Logger;
import pro.gravit.launcher.base.config.JsonConfigurable;
import pro.gravit.launcher.base.request.RequestException;
import pro.gravit.launcher.base.request.RequestOfflineModeAPIImpl;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launcher.runtime.client.DirBridge;
import pro.gravit.utils.helper.ManagedPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Supplies the local profile/update view used by offline authentication. */
final class OfflineProfileService {
    private final Logger logger;
    private OfflineModeStorageData storage;
    private JsonConfigurable<OfflineModeStorageData> storageConfig;

    OfflineProfileService(Logger logger) {
        this.logger = logger;
    }

    void initialize(Path storagePath) throws IOException {
        storageConfig = new JsonConfigurable<>(OfflineModeStorageData.class, storagePath) {
            @Override public OfflineModeStorageData getConfig() { return storage; }
            @Override public void setConfig(OfflineModeStorageData config) { storage = config; }
            @Override public OfflineModeStorageData getDefaultConfig() { return new OfflineModeStorageData(); }
        };
        storageConfig.generateConfigIfNotExists();
        storageConfig.loadConfig();
    }

    OfflineModeStorageData storage() { return storage; }

    void updateProfiles(List<ProfileFeatureAPI.ClientProfile> profiles) {
        if (storage != null) storage.profiles = profiles.stream().map(profile -> (pro.gravit.launcher.base.profiles.ClientProfile) profile).toList();
    }

    void save() {
        if (storageConfig == null) return;
        try {
            storageConfig.saveConfig();
        } catch (IOException e) {
            logger.error("OfflineConfig not saved", e);
        }
    }

    void configure(RequestOfflineModeAPIImpl api, OfflineModeStorageData storage) {
        if (storage == null || storage.profiles == null) return;
        api.setOfflineProfiles(new ArrayList<>(storage.profiles));
        api.setOfflineUpdateProvider(dirName -> CompletableFuture.supplyAsync(() -> {
            Path path;
            try {
                path = ManagedPath.resolve(DirBridge.dirUpdates, dirName);
            } catch (IOException e) {
                throw new RequestException(String.format("Invalid update directory '%s'", dirName), e);
            }
            if (Files.notExists(path)) throw new RequestException(String.format("Dir '%s' not found", path));
            try {
                HashedDir dir = new HashedDir(path, null, false, true);
                return new ProfileFeatureAPI.UpdateInfo() {
                    @Override public HashedDir getHashedDir() { return dir; }
                    @Override public String getUrl() { return ""; }
                };
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
