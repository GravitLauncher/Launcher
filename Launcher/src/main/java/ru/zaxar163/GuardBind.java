package ru.zaxar163;

import ru.gravit.launcher.LauncherAPI;

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

    @LauncherAPI
    public static native int getCheckTime();
    
    @LauncherAPI
    public static native void setCheckTime(int time);
}
