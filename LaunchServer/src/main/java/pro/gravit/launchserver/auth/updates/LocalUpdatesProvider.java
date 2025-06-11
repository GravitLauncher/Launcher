package pro.gravit.launchserver.auth.updates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LocalUpdatesProvider extends UpdatesProvider {
    private transient final Logger logger = LogManager.getLogger();
    public String updatesDir = "updates";
    public String binaryName = "Launcher";
    public Map<UpdateVariant, String> urls = new HashMap<>(Map.of(
            UpdateVariant.JAR, "http://localhost:9274/Launcher.jar",
            UpdateVariant.EXE, "http://localhost:9274/Launcher.exe"
    ));
    private final transient Map<UpdateVariant, byte[]> hashMap = new HashMap<>();

    @Override
    public void init(LaunchServer server) {
        super.init(server);
        try {
            sync(UpdateVariant.JAR);
            sync(UpdateVariant.EXE);
        } catch (IOException e) {
            logger.error("Error when syncing binaries", e);
        }
    }

    @Override
    public void pushUpdate(Map<UpdateVariant, Path> files) throws IOException {
        for(var e : files.entrySet()) {
            IOHelper.copy(e.getValue(), getUpdate(e.getKey()));
            sync(e.getKey());
        }
    }

    public void sync(UpdateVariant variant) throws IOException {
        var source = getUpdate(variant);
        if(!Files.exists(source)) {
            logger.warn("Dont exist {} binary", variant);
            return;
        }
        byte[] hash = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA512, source);
        hashMap.put(variant, hash);
    }

    public Path getUpdate(UpdateVariant variant) {
        String fileName;
        switch (variant) {
            case JAR -> {
                fileName = binaryName.concat(".jar");
            }
            case EXE -> {
                fileName = binaryName.concat(".exe");
            }
            default -> {
                fileName = binaryName;
            }
        }
        return Path.of(updatesDir).resolve(fileName);
    }

    @Override
    public UpdateInfo checkUpdates(UpdateVariant variant, byte[] digest) {
        byte[] hash = hashMap.get(variant);
        if (hash == null) {
            return null; // We dont have this file
        }
        if(Arrays.equals(digest, hash)) {
            return null; // Launcher already updated
        }
        return new UpdateInfo(urls.get(variant));
    }
}
