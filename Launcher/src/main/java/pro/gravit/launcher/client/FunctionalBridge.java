package pro.gravit.launcher.client;

import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.hasher.FileNameMatcher;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.serialize.signed.SignedObjectHolder;
import pro.gravit.launcher.HWID;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.guard.LauncherGuardManager;
import pro.gravit.launcher.hwid.OshiHWIDProvider;
import pro.gravit.launcher.managers.ConsoleManager;
import pro.gravit.launcher.managers.HasherManager;
import pro.gravit.launcher.managers.HasherStore;
import pro.gravit.launcher.request.Request;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FunctionalBridge {
    @LauncherAPI
    public static ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(0);
    @LauncherAPI
    public static OshiHWIDProvider hwidProvider = new OshiHWIDProvider();
    @LauncherAPI
    public static AtomicReference<HWID> hwid = new AtomicReference<>();
    @LauncherAPI
    public static Thread getHWID = null;

    private static long cachedMemorySize = -1;

    @LauncherAPI
    public static HashedDirRunnable offlineUpdateRequest(String dirName, Path dir, SignedObjectHolder<HashedDir> hdir, FileNameMatcher matcher, boolean digest) {
        return () -> {
            if (hdir == null) {
                Request.requestError(java.lang.String.format("Директории '%s' нет в кэше", dirName));
            }
            ClientLauncher.verifyHDir(dir, hdir.object, matcher, digest);
            return hdir;
        };
    }

    @LauncherAPI
    public static void startTask(Runnable task) {
        threadPool.execute(task);
    }

    @LauncherAPI
    public static HWID getHWID() {
        HWID hhwid = hwid.get();
        if (hhwid == null) hwid.set(hwidProvider.getHWID());
        return hhwid;
    }

    @LauncherAPI
    public static int getTotalMemory() {
        if (cachedMemorySize > 0) return (int) cachedMemorySize;
        return (int) (cachedMemorySize = hwidProvider.getTotalMemory() >> 20);
    }

    @LauncherAPI
    public static int getClientJVMBits() {
        return LauncherGuardManager.guard.getClientJVMBits();
    }

    @LauncherAPI
    public static int getJVMTotalMemory() {
        if (getClientJVMBits() == 32) {
            return Math.min(getTotalMemory(), 1536);
        } else {
            return getTotalMemory();
        }
    }

    @LauncherAPI
    public static HasherStore getDefaultHasherStore() {
        return HasherManager.getDefaultStore();
    }

    @LauncherAPI
    public static void registerUserSettings(String typename, Class<? extends UserSettings> clazz) {
        UserSettings.providers.register(typename, clazz);
    }

    @LauncherAPI
    public static void close() throws Exception {
        threadPool.awaitTermination(2, TimeUnit.SECONDS);
    }

    @LauncherAPI
    public static void setAuthParams(AuthRequestEvent event) {
        if (event.session != 0) {
            Request.setSession(event.session);
        }
        LauncherGuardManager.guard.setProtectToken(event.protectToken);
    }

    @FunctionalInterface
    public interface HashedDirRunnable {
        SignedObjectHolder<HashedDir> run() throws Exception;
    }

    @LauncherAPI
    public static void evalCommand(String cmd) {
        ConsoleManager.handler.eval(cmd, false);
    }

    @LauncherAPI
    public static void addPlainOutput(LogHelper.Output output) {
        LogHelper.addOutput(output, LogHelper.OutputTypes.PLAIN);
    }
}
