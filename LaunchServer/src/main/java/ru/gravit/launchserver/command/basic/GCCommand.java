package ru.gravit.launchserver.command.basic;

import ru.gravit.launcher.managers.GarbageManager;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;

public final class GCCommand extends Command {
    public GCCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Perform Garbage Collection and print memory usage";
    }

    @Override
    public void invoke(String... args) {
        LogHelper.subInfo("Performing full GC");
        JVMHelper.fullGC();
        GarbageManager.gc();
        // Print memory usage
        long max = JVMHelper.RUNTIME.maxMemory() >> 20;
        long free = JVMHelper.RUNTIME.freeMemory() >> 20;
        long total = JVMHelper.RUNTIME.totalMemory() >> 20;
        long used = total - free;
        LogHelper.subInfo("Heap usage: %d / %d / %d MiB", used, total, max);
    }
}
