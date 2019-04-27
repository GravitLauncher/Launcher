package ru.gravit.launcher.client;

import ru.gravit.launcher.HWID;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.events.request.AuthRequestEvent;
import ru.gravit.launcher.guard.LauncherGuardManager;
import ru.gravit.launcher.hasher.FileNameMatcher;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.hwid.OshiHWIDProvider;
import ru.gravit.launcher.managers.ConsoleManager;
import ru.gravit.launcher.managers.HasherManager;
import ru.gravit.launcher.managers.HasherStore;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.utils.helper.LogHelper;

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
    public static void makeJsonRequest(RequestInterface request, Runnable callback) {

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
    public static long getTotalMemory() {
    	if (cachedMemorySize > 0) return cachedMemorySize;
    	return cachedMemorySize = hwidProvider.getTotalMemory() >> 20;
    }

    @LauncherAPI
    public static int getClientJVMBits() {
        return LauncherGuardManager.guard.getClientJVMBits();
    }

    @LauncherAPI
    public static long getJVMTotalMemory() {
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
    public static void registerUserSettings(String typename, Class<? extends UserSettings> clazz)
    {
        UserSettings.providers.register(typename, clazz);
    }
    @LauncherAPI
    public static void close() throws Exception
    {
        threadPool.awaitTermination(2, TimeUnit.SECONDS);
    }

    @LauncherAPI
    public static void setAuthParams(AuthRequestEvent event) {
        if(event.session != 0)
        {
            Request.setSession(event.session);
        }
        LauncherGuardManager.guard.setProtectToken(event.protectToken);
    }

    @FunctionalInterface
    public interface HashedDirRunnable {
        SignedObjectHolder<HashedDir> run() throws Exception;
    }
    @LauncherAPI
    public static void evalCommand(String cmd)
    {
        ConsoleManager.handler.eval(cmd, false);
    }
    @LauncherAPI
    public static void addPlainOutput(LogHelper.Output output)
    {
        LogHelper.addOutput(output, LogHelper.OutputTypes.PLAIN);
    }
}
