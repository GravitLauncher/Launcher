package pro.gravit.launchserver.binary;

import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;

import java.io.IOException;
import java.nio.file.Path;

public abstract class LauncherBinary extends BinaryPipeline {
    public final LaunchServer server;
    public final PipelineContext context;

    protected LauncherBinary(LaunchServer server) {
        this.server = server;
        this.context = new PipelineContext(server);
    }

    public PipelineContext build() throws IOException {
        logger.info("Building launcher binary file");
        long time_start = System.currentTimeMillis();
        long time_this = time_start;
        for (LauncherBuildTask task : tasks) {
            logger.info("Task {}", task.getName());
            Path newPath = task.process(context);
            if(newPath != null) {
                context.setLastest(newPath);
                context.putArtifact(task.getName(), newPath);
            }
            long time_task_end = System.currentTimeMillis();
            long time_task = time_task_end - time_this;
            time_this = time_task_end;
            logger.info("Task {} processed from {} millis", task.getName(), time_task);
        }
        long time_end = System.currentTimeMillis();
        logger.info("Build successful from {} millis", time_end - time_start);
        return this.context;
    }

    public abstract CoreFeatureAPI.UpdateVariant getVariant();

    public void init() {
    }
}
