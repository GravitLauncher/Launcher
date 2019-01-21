package ru.gravit.launchserver.binary.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class TaskUtil {
    public static void addCounted(List<LauncherBuildTask> tasks, int count, Predicate<LauncherBuildTask> pred, LauncherBuildTask taskAdd) {
        List<LauncherBuildTask> indexes = new ArrayList<>();
        tasks.stream().filter(pred).forEach(e -> indexes.add(e));
        indexes.forEach(e -> tasks.add(tasks.indexOf(e) + count, taskAdd));
    }

    public static void replaceCounted(List<LauncherBuildTask> tasks, int count, Predicate<LauncherBuildTask> pred, LauncherBuildTask taskRep) {
        List<LauncherBuildTask> indexes = new ArrayList<>();
        tasks.stream().filter(pred).forEach(e -> indexes.add(e));
        indexes.forEach(e -> tasks.set(tasks.indexOf(e) + count, taskRep));
    }

    public static void addPre(List<LauncherBuildTask> tasks, Predicate<LauncherBuildTask> pred, LauncherBuildTask taskAdd) {
        addCounted(tasks, -1, pred, taskAdd);
    }

    public static void add(List<LauncherBuildTask> tasks, Predicate<LauncherBuildTask> pred, LauncherBuildTask taskAdd) {
        addCounted(tasks, 0, pred, taskAdd);
    }

    public static void addAfter(List<LauncherBuildTask> tasks, Predicate<LauncherBuildTask> pred, LauncherBuildTask taskAdd) {
        addCounted(tasks, 1, pred, taskAdd);
    }

    public static void replacePre(List<LauncherBuildTask> tasks, Predicate<LauncherBuildTask> pred, LauncherBuildTask taskRep) {
        replaceCounted(tasks, -1, pred, taskRep);
    }

    public static void replace(List<LauncherBuildTask> tasks, Predicate<LauncherBuildTask> pred, LauncherBuildTask taskRep) {
        replaceCounted(tasks, 0, pred, taskRep);
    }

    public static void replaceAfter(List<LauncherBuildTask> tasks, Predicate<LauncherBuildTask> pred, LauncherBuildTask taskRep) {
        replaceCounted(tasks, 1, pred, taskRep);
    }

    public static <T extends LauncherBuildTask> List<T> getTaskByClass(List<LauncherBuildTask> tasks, Class<T> taskClass) {
        return tasks.stream().filter(taskClass::isInstance).map(taskClass::cast).collect(Collectors.toList());
    }

    private TaskUtil() {
    }
}
