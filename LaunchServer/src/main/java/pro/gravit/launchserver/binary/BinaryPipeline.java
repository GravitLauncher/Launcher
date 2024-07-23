package pro.gravit.launchserver.binary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BinaryPipeline {
    public final List<LauncherBuildTask> tasks = new ArrayList<>();
    public final Path buildDir;
    public final String nameFormat;
    private transient final Logger logger = LogManager.getLogger();

    public BinaryPipeline(Path buildDir, String nameFormat) {
        this.buildDir = buildDir;
        this.nameFormat = nameFormat;
    }

    public void addCounted(int count, Predicate<LauncherBuildTask> pred, LauncherBuildTask taskAdd) {
        List<LauncherBuildTask> indexes = new ArrayList<>();
        tasks.stream().filter(pred).forEach(indexes::add);
        indexes.forEach(e -> tasks.add(tasks.indexOf(e) + count, taskAdd));
    }

    public void replaceCounted(int count, Predicate<LauncherBuildTask> pred, LauncherBuildTask taskRep) {
        List<LauncherBuildTask> indexes = new ArrayList<>();
        tasks.stream().filter(pred).forEach(indexes::add);
        indexes.forEach(e -> tasks.set(tasks.indexOf(e) + count, taskRep));
    }

    public void addPre(Predicate<LauncherBuildTask> pred, LauncherBuildTask taskAdd) {
        addCounted(-1, pred, taskAdd);
    }

    public void add(Predicate<LauncherBuildTask> pred, LauncherBuildTask taskAdd) {
        addCounted(0, pred, taskAdd);
    }

    public void addAfter(Predicate<LauncherBuildTask> pred, LauncherBuildTask taskAdd) {
        addCounted(1, pred, taskAdd);
    }

    public void replacePre(Predicate<LauncherBuildTask> pred, LauncherBuildTask taskRep) {
        replaceCounted(-1, pred, taskRep);
    }

    public void replace(Predicate<LauncherBuildTask> pred, LauncherBuildTask taskRep) {
        replaceCounted(0, pred, taskRep);
    }

    public void replaceAfter(Predicate<LauncherBuildTask> pred, LauncherBuildTask taskRep) {
        replaceCounted(1, pred, taskRep);
    }

    public <T extends LauncherBuildTask> List<T> getTasksByClass(Class<T> taskClass) {
        return tasks.stream().filter(taskClass::isInstance).map(taskClass::cast).collect(Collectors.toList());
    }

    public <T extends LauncherBuildTask> Optional<T> getTaskByClass(Class<T> taskClass) {
        return tasks.stream().filter(taskClass::isInstance).map(taskClass::cast).findFirst();
    }

    public Optional<LauncherBuildTask> getTaskBefore(Predicate<LauncherBuildTask> pred) {
        LauncherBuildTask last = null;
        for(var e : tasks) {
            if(pred.test(e)) {
                return Optional.ofNullable(last);
            }
            last = e;
        }
        return Optional.empty();
    }

    public void build(Path target, boolean deleteTempFiles) throws IOException {
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
        if (deleteTempFiles) IOHelper.move(thisPath, target);
        else IOHelper.copy(thisPath, target);
        IOHelper.deleteDir(buildDir, false);
        logger.info("Build successful from {} millis", time_end - time_start);
    }

    public String nextName(String taskName) {
        return nameFormat.formatted(taskName);
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
