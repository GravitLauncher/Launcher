package pro.gravit.launchserver.auth.updates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.config.JsonConfigurable;
import pro.gravit.launcher.base.config.SimpleConfigurable;
import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LocalUpdatesProvider extends UpdatesProvider {
    private transient final Logger logger = LogManager.getLogger();
    public String updatesDir = "updates";
    public String binaryName = "Launcher";
    public String buildSecretsFile = "build-secrets.json";
    public Map<CoreFeatureAPI.UpdateVariant, String> urls = new HashMap<>(Map.of(
            CoreFeatureAPI.UpdateVariant.JAR, "http://localhost:9274/Launcher.jar",
            CoreFeatureAPI.UpdateVariant.EXE_WINDOWS_X86_64, "http://localhost:9274/Launcher.exe"
    ));
    public transient JsonConfigurable<BuildSecretsInfo> buildSecretsJson;
    private final transient Map<CoreFeatureAPI.UpdateVariant, byte[]> hashMap = new HashMap<>();

    @Override
    public void init(LaunchServer server) {
        super.init(server);
        buildSecretsJson = new SimpleConfigurable<>(BuildSecretsInfo.class, Path.of(buildSecretsFile));
        if(server.env == LaunchServer.LaunchServerEnv.TEST) {
            return;
        }
        try {
            buildSecretsJson.generateConfigIfNotExists();
            buildSecretsJson.loadConfig();
        } catch (Exception e) {
            buildSecretsJson.setConfig(buildSecretsJson.getDefaultConfig());
        }
        try {
            sync(CoreFeatureAPI.UpdateVariant.JAR);
            sync(CoreFeatureAPI.UpdateVariant.EXE_WINDOWS_X86_64);
        } catch (IOException e) {
            logger.error("Error when syncing binaries", e);
        }
    }

    @Override
    public void pushUpdate(List<UpdateUploadInfo> files) throws IOException {
        for(var e : files) {
            IOHelper.copy(e.path(), getUpdate(e.variant()));
            buildSecretsJson.getConfig().secrets().put(e.variant(), e.secrets());
            sync(e.variant());
        }
        buildSecretsJson.saveConfig();
    }

    public void sync(CoreFeatureAPI.UpdateVariant variant) throws IOException {
        var source = getUpdate(variant);
        if(!Files.exists(source)) {
            logger.warn("Dont exist {} binary", variant);
            return;
        }
        byte[] hash = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA512, source);
        hashMap.put(variant, hash);
    }

    public Path getUpdate(CoreFeatureAPI.UpdateVariant variant) {
        String fileName;
        switch (variant) {
            case JAR -> {
                fileName = binaryName.concat(".jar");
            }
            case EXE_WINDOWS_X86_64 -> {
                fileName = binaryName.concat(".exe");
            }
            case EXE_WINDOWS_X86 -> {
                fileName = binaryName.concat("_x32.exe");
            }
            case EXE_WINDOWS_ARM64 -> {
                fileName = binaryName.concat("_arm64.exe");
            }
            case LINUX_X86_64 -> {
                fileName = binaryName.concat("_linux");
            }
            case LINUX_ARM64 -> {
                fileName = binaryName.concat("_linux_arm64");
            }
            case MACOS_X86_64 -> {
                fileName = binaryName.concat("_macos");
            }
            case MACOS_ARM64 -> {
                fileName = binaryName.concat("_macos_arm64");
            }
            default -> {
                fileName = binaryName;
            }
        }
        return Path.of(updatesDir).resolve(fileName);
    }

    @Override
    public UpdateInfo checkUpdates(CoreFeatureAPI.UpdateVariant variant, BuildSecretsCheck buildSecretsCheck) {
        byte[] hash = hashMap.get(variant);
        if (hash == null) {
            return null; // We dont have this file
        }
        if(checkSecureHash(buildSecretsCheck.secureHash(), buildSecretsCheck.secureSalt(), buildSecretsJson.getConfig().secrets().get(variant).secureToken()) && Arrays.equals(buildSecretsCheck.digest(), hash)) {
            return null; // Launcher already updated
        }
        return new UpdateInfo(urls.get(variant));
    }

    public static final class BuildSecretsInfo {
        private Map<CoreFeatureAPI.UpdateVariant, BuildSecrets> secrets = new HashMap<>();

        public BuildSecretsInfo(Map<CoreFeatureAPI.UpdateVariant, BuildSecrets> secrets) {
            this.secrets = secrets;
        }

        public BuildSecretsInfo() {

        }

        public Map<CoreFeatureAPI.UpdateVariant, BuildSecrets> secrets() {
            return secrets;
        }


        }
}
