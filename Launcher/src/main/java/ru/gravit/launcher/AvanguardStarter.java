package ru.gravit.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import cpw.mods.fml.SafeExitJVMLegacy;
import net.minecraftforge.fml.SafeExitJVM;
import ru.gravit.launcher.hasher.DirWatcher;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.utils.NativeJVMHalt;
import ru.gravit.utils.helper.*;
import ru.zaxar163.GuardBind;

public class AvanguardStarter {
    static class SecurityThread implements Runnable {
        static long macID = GuardBind.avnGetMacId();

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    if (macID != GuardBind.avnGetMacId()) {
                        LogHelper.error("MacID changed");
                        safeHalt(8);
                    }
                    if (!GuardBind.avnIsStarted()) {
                        LogHelper.error("Avanguard stopped! Process stopped");
                        safeHalt(5);
                    }
                } catch (NullPointerException e) {
                    LogHelper.error("Avanguard unloaded! Process stopped");
                    safeHalt(6);
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    GuardBind.avnGetMacId();
                    if (!GuardBind.avnIsStarted()) {
                        LogHelper.error("Thread stopped! Process stopped");
                        safeHalt(7);
                    }
                }
            }
        }
    }

    static void safeHalt(int exitcode) {
        try {
            SafeExitJVMLegacy.exit(exitcode);
        } catch (Throwable ignored) {

        }
        try {
            SafeExitJVM.exit(exitcode);
        } catch (Throwable ignored) {

        }
        NativeJVMHalt halt = new NativeJVMHalt(exitcode);
        halt.halt();
    }

    public static final String NAME = Launcher.getConfig().projectname;
    public static Path wrapper = null, avanguard = null;

    public static void main(boolean init) {
        if (init)
            GuardBind.init();
        GuardBind.avnRegisterThreatNotifier((int threatType) -> {
            System.err.println("Threat " + GuardBind.ThreatType.getThreat(threatType).name());
            LogHelper.error("Cheating == crash!");
            try {
                SafeExitJVM.exit(threatType + 7000);
            } catch (Throwable e) {
                SafeExitJVMLegacy.exit(threatType + 7000);
            }
            return false;
        });
        // нужно делать до пуска таймера!
        GuardBind.avnStartDefence();
        CommonHelper.newThread("Security Thread", true, new SecurityThread()).start();
    }

    public static void load() {
        GuardBind.startAbs(avanguard.toString());
    }

    public static void start(Path path1) throws IOException {
        Path path = path1.resolve("guard");
        if (!IOHelper.exists(path))
            Files.createDirectories(path);
        Path avanguard = path.resolve(JVMHelper.JVM_BITS == 64 ? "Avanguard64.dll" : "Avanguard32.dll");
        Path wrapper = path.resolve(JVMHelper.JVM_BITS == 64 ? NAME + "64.exe" : NAME + "32.exe");
        String avanguardResource = JVMHelper.JVM_BITS == 64 ? "Avanguard64.dll" : "Avanguard32.dll";
        String wrapperResource = JVMHelper.JVM_BITS == 64 ? "wrapper64.exe" : "wrapper32.exe";
        UnpackHelper.unpack(Launcher.getResourceURL(avanguardResource, "guard"), avanguard);
        UnpackHelper.unpack(Launcher.getResourceURL(wrapperResource, "guard"), wrapper);
        AvanguardStarter.wrapper = wrapper;
        AvanguardStarter.avanguard = avanguard;
        HashedDir guard = new HashedDir(path, null, true, false);
        DirWatcher dirWatcher = new DirWatcher(path, guard, null, false);
        CommonHelper.newThread("Guard Directory Watcher", true, dirWatcher).start();
    }
}
