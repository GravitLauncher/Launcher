package pro.gravit.launchserver.binary;

import pro.gravit.launcher.Launcher;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.*;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class JARLauncherBinary extends LauncherBinary {
    public final AtomicLong count;
    public final Path runtimeDir;
    public final Path guardDir;
    public final Path buildDir;
    public final List<LauncherBuildTask> tasks;
    public final List<Path> coreLibs;
    public final List<Path> addonLibs;

    public JARLauncherBinary(LaunchServer server) throws IOException {
        super(server, resolve(server, ".jar"));
        count = new AtomicLong(0);
        runtimeDir = server.dir.resolve(Launcher.RUNTIME_DIR);
        guardDir = server.dir.resolve(Launcher.GUARD_DIR);
        buildDir = server.dir.resolve("build");
        tasks = new ArrayList<>();
        coreLibs = new ArrayList<>();
        addonLibs = new ArrayList<>();
        if (!Files.isDirectory(buildDir)) {
            Files.deleteIfExists(buildDir);
            Files.createDirectory(buildDir);
        }
    }

    @Override
    public void init() {
        tasks.add(new PrepareBuildTask(server));
        tasks.add(new MainBuildTask(server));
        if (server.config.launcher.attachLibraryBeforeProGuard) tasks.add(new AttachJarsTask(server));
        tasks.add(new ProGuardBuildTask(server));
        tasks.add(new AdditionalFixesApplyTask(server));
        if (!server.config.launcher.attachLibraryBeforeProGuard) tasks.add(new AttachJarsTask(server));
        if (server.config.launcher.compress) tasks.add(new CompressBuildTask(server));
        if(server.config.sign.enabled) tasks.add(new SignJarTask(server.config.sign, server));
    }

    @Override
    public void build() throws IOException {
        LogHelper.info("Building launcher binary file");
        count.set(0); // set jar number
        Path thisPath = null;
        boolean isNeedDelete = false;
        long time_start = System.currentTimeMillis();
        long time_this = time_start;
        for (LauncherBuildTask task : tasks) {
            LogHelper.subInfo("Task %s", task.getName());
            Path oldPath = thisPath;
            thisPath = task.process(oldPath);
            long time_task_end = System.currentTimeMillis();
            long time_task = time_task_end - time_this;
            time_this = time_task_end;
            if (isNeedDelete && server.config.launcher.deleteTempFiles) Files.deleteIfExists(oldPath);
            isNeedDelete = task.allowDelete();
            LogHelper.subInfo("Task %s processed from %d millis", task.getName(), time_task);
        }
        long time_end = System.currentTimeMillis();
        if (isNeedDelete && server.config.launcher.deleteTempFiles) IOHelper.move(thisPath, syncBinaryFile);
        else IOHelper.copy(thisPath, syncBinaryFile);
        LogHelper.info("Build successful from %d millis", time_end - time_start);
    }

    public String nextName(String taskName) {
        return String.format("Launcher-%s-%d.jar", taskName, count.getAndIncrement());
    }

    public Path nextPath(String taskName) {
        return buildDir.resolve(nextName(taskName));
    }

    public Path nextPath(LauncherBuildTask task) {
        return nextPath(task.getName());
    }

    public Path nextLowerPath(LauncherBuildTask task) {
        return nextPath(CommonHelper.low(task.getName()));
    }
}
