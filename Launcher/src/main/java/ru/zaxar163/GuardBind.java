package ru.zaxar163;

import java.nio.file.Path;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;

@LauncherAPI
public final class GuardBind {
    public interface ThreatNotifier {
        boolean call(int threatType);
    }

    public enum ThreatType {
        UNKNOWN_THREAT(0),
        REMOTE_THREAD(1),
        WINDOWS_HOOKS_INJECTION(2),
        UNKNOWN_TRACE_LOAD_LIBRARY(3),
        CONTEXT_MANIPULATION(4),
        CRITICAL_MODULE_CHANGED(5),
        UNKNOWN_INTERCEPTION(6),
        UNKNOWN_MEMORY_REGION(7),
        UNKNOWN_APC_DESTINATION(8);

        public static ThreatType getThreat(int threatType) {
            return ThreatType.values()[threatType];
        }

        private final int id;

        ThreatType(int value) {
            id = value;
        }

        public int getValue() {
            return id;
        }
    }

    @LauncherAPI
    public static native void avnEliminateThreat(int threatType);

    @LauncherAPI
    public static native long avnGetCpuid();

    @LauncherAPI
    public static native long avnGetHash(byte[] data);

    @LauncherAPI
    public static native long avnGetHddId();

    @LauncherAPI
    public static native long avnGetMacId();

    @LauncherAPI
    public static native long avnGetSmbiosId();

    @LauncherAPI
    public static native boolean avnIsStarted();

    @LauncherAPI
    public static native boolean avnIsStaticLoaded();

    @LauncherAPI
    public static native void avnRegisterThreatNotifier(ThreatNotifier notifier);

    @LauncherAPI
    public static native boolean avnStartDefence();

    @LauncherAPI
    public static native void avnStopDefence();

    public static void init() {
        LogHelper.debug("Anti-Cheat loading");
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE)
			if (JVMHelper.JVM_BITS == 32)
				System.loadLibrary("Avanguard32");
			else if (JVMHelper.JVM_BITS == 64)
				System.loadLibrary("Avanguard64");
        LogHelper.debug("Anti-Cheat loaded");
    }

    public static void start(Path path) {
        LogHelper.debug("Anti-Cheat loading");
        System.load(path.normalize().toAbsolutePath().toFile().getAbsolutePath());
        LogHelper.debug("Anti-Cheat loaded");
    }

    public static void startAbs(String path) {
        LogHelper.debug("Anti-Cheat loading");
        System.load(path);
        LogHelper.debug("Anti-Cheat loaded");
    }
}
