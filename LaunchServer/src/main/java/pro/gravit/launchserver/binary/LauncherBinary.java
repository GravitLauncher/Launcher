package pro.gravit.launchserver.binary;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.updates.UpdatesProvider;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public abstract class LauncherBinary extends BinaryPipeline {
    public final LaunchServer server;

    protected LauncherBinary(LaunchServer server, String nameFormat) {
        super(server.tmpDir.resolve("build"), nameFormat);
        this.server = server;
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
        if(thisPath != null) {
            // TODO fix me
            server.config.updatesProvider.pushUpdate(List.of(new UpdatesProvider.UpdateUploadInfo(thisPath, getVariant(), new UpdatesProvider.BuildSecrets(server.runtime.clientCheckSecret, null))));
        } else {
            logger.warn("Missing {} binary file", getVariant());
        }
        IOHelper.deleteDir(buildDir, false);
        logger.info("Build successful from {} millis", time_end - time_start);
    }

    public abstract UpdatesProvider.UpdateVariant getVariant();

    public void init() {
    }
}
