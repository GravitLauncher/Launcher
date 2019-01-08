package ru.gravit.launchserver.binary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import ru.gravit.launcher.Launcher;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.binary.tasks.LauncherBuildTask;
import ru.gravit.launchserver.binary.tasks.MainBuildTask;
import ru.gravit.launchserver.binary.tasks.ProGuardBuildTask;
import ru.gravit.launchserver.binary.tasks.StripLineNumbersTask;
import ru.gravit.launchserver.binary.tasks.UnpackBuildTask;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

public final class JARLauncherBinary extends LauncherBinary {
    public ArrayList<LauncherBuildTask> tasks;
	public final AtomicLong count;
    public final Path runtimeDir;
    public final Path guardDir;
    public final Path buildDir;

    public JARLauncherBinary(LaunchServer server) throws IOException {
        super(server);
        tasks = new ArrayList<>();
        tasks.add(new UnpackBuildTask(server));
        tasks.add(new MainBuildTask(server));
        if(server.config.enabledProGuard) tasks.add(new ProGuardBuildTask(server));
        if(server.config.stripLineNumbers) tasks.add(new StripLineNumbersTask(server));
        count = new AtomicLong(0);
        syncBinaryFile = server.dir.resolve(server.config.binaryName + ".jar");
        runtimeDir = server.dir.resolve(Launcher.RUNTIME_DIR);
        guardDir = server.dir.resolve(Launcher.GUARD_DIR);
        buildDir = server.dir.resolve("build");
    }

    @Override
    public void build() throws IOException {
        LogHelper.info("Building launcher binary file");
        count.set(0);
        Path thisPath = null;
        boolean isNeedDelete = false;
        long time_start = System.currentTimeMillis();
        long time_this = time_start;
        for(LauncherBuildTask task : tasks)
        {
            LogHelper.subInfo("Task %s",task.getName());
            Path oldPath = thisPath;
            thisPath = task.process(oldPath);
            long time_task_end = System.currentTimeMillis();
            long time_task = time_task_end - time_this;
            time_this = time_task_end;
            if (isNeedDelete) Files.delete(oldPath);
            isNeedDelete = task.allowDelete();
            LogHelper.subInfo("Task %s processed from %d millis",task.getName(), time_task);
        }
        long time_end = System.currentTimeMillis();
        IOHelper.move(thisPath, syncBinaryFile);
        LogHelper.info("Build successful from %d millis",time_end - time_start);
    }
    
    public String nextName(String taskName) {
    	return String.format("%s-%s-%d.jar", server.config.projectName, taskName, count.getAndIncrement());
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
