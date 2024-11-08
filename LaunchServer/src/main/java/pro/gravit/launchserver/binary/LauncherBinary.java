package pro.gravit.launchserver.binary;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public abstract class LauncherBinary extends BinaryPipeline {
    public final LaunchServer server;
    public final Path syncBinaryFile;
    private volatile byte[] digest;

    protected LauncherBinary(LaunchServer server, Path binaryFile, String nameFormat) {
        super(server.tmpDir.resolve("build"), nameFormat);
        this.server = server;
        syncBinaryFile = binaryFile;
    }

    public static Path resolve(LaunchServer server, String ext) {
        return Path.of(server.config.binaryName + ext);
    }

    public void build() throws IOException {
        logger.info("Building launcher binary file");
        Path thisPath = null;
        long time_start = System.currentTimeMillis();
        long time_this = time_start;
        for (LauncherBuildTask task : tasks) {
            logger.info("Task {}", task.getName());
            Path oldPath = thisPath;
            thisPath = task.process(oldPath);
            long time_task_end = System.currentTimeMillis();
            long time_task = time_task_end - time_this;
            time_this = time_task_end;
            logger.info("Task {} processed from {} millis", task.getName(), time_task);
        }
        long time_end = System.currentTimeMillis();
        server.config.updatesProvider.upload(null, Map.of(syncBinaryFile.toString(), thisPath), true);
        IOHelper.deleteDir(buildDir, false);
        logger.info("Build successful from {} millis", time_end - time_start);
    }

    public final boolean exists() {
        return syncBinaryFile != null && IOHelper.isFile(syncBinaryFile);
    }

    public final byte[] getDigest() {
        return digest;
    }

    public void init() {
    }

    public final boolean sync() throws IOException {
        try {
            var target = syncBinaryFile.toString();
            var path = server.config.updatesProvider.download(null, List.of(target)).get(target);
            digest = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA512, IOHelper.read(path));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
