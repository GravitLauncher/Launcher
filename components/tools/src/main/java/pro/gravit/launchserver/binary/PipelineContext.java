package pro.gravit.launchserver.binary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.updates.UpdatesProvider;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PipelineContext {
    private final Logger logger = LogManager.getLogger();
    private final LaunchServer launchServer;
    private final Map<String, Path> artifacts = new HashMap<>();
    private final Map<String, Object> properties = new HashMap<>();
    private final Set<Path> tempFiles = new HashSet<>();
    private Path lastest;

    public PipelineContext(LaunchServer launchServer) {
        this.launchServer = launchServer;
    }

    public Path makeTempPath(String name, String ext) throws IOException {
        return this.launchServer.createTempFilePath(name, ext);
    }

    public Map<String, Path> getArtifacts() {
        return artifacts;
    }

    public Path getLastest() {
        return lastest;
    }

    public void setLastest(Path lastest) {
        this.lastest = lastest;
    }

    public void putArtifact(String name, Path path) {
        artifacts.put(name, path);
    }

    public void putProperty(String name, Object prop) {
        properties.put(name, prop);
    }

    public void putProperties(Map<String, Object> properties) {
        this.properties.putAll(properties);
    }

    public LaunchServer getLaunchServer() {
        return launchServer;
    }

    @SuppressWarnings("unchecked")
    public<T> T getProperty(String name) {
        return (T) properties.get(name);
    }

    public UpdatesProvider.UpdateUploadInfo makeUploadInfo(CoreFeatureAPI.UpdateVariant variant) {
        if(getLastest() == null) {
            return null;
        }
        try {
            byte[] hash = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA512, getLastest());
            return new UpdatesProvider.UpdateUploadInfo(getLastest(), variant, new UpdatesProvider.BuildSecrets(
                    getProperty("checkClientSecret"), hash,
                    getProperty("build.privateKey"),
                    getProperty("build.publicKey")
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clear() {
        for(var p : tempFiles) {
            if(Files.exists(p)) {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    logger.error("Couldn't delete file {}", p, e);
                }
            }
        }
        tempFiles.clear();
        properties.clear();
        artifacts.clear();
    }
}
