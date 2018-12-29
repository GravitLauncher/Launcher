package ru.gravit.launcher.client;

import javafx.concurrent.Task;
import ru.gravit.launcher.HWID;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.RequestWorker;
import ru.gravit.launcher.hasher.FileNameMatcher;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.hwid.OshiHWIDProvider;
import ru.gravit.launcher.managers.HasherManager;
import ru.gravit.launcher.managers.HasherStore;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.update.LegacyLauncherRequest;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SignatureException;
import java.util.concurrent.atomic.AtomicReference;

public class FunctionalBridge {
    @LauncherAPI
    public static LauncherSettings settings;
    @LauncherAPI
    public static RequestWorker worker;
    @LauncherAPI
    public static OshiHWIDProvider hwidProvider = new OshiHWIDProvider();
    @LauncherAPI
    public static AtomicReference<HWID> hwid = new AtomicReference<>();

    @LauncherAPI
    public static HashedDirRunnable offlineUpdateRequest(String dirName, Path dir, SignedObjectHolder<HashedDir> hdir, FileNameMatcher matcher, boolean digest) throws Exception {
        return () -> {
            if (hdir == null) {
                Request.requestError(java.lang.String.format("Директории '%s' нет в кэше", dirName));
            }
            ClientLauncher.verifyHDir(dir, hdir.object, matcher, digest);
            return hdir;
        };
    }

    @LauncherAPI
    public static LegacyLauncherRequest.Result offlineLauncherRequest() throws IOException, SignatureException {
        if (settings.lastDigest == null || settings.lastProfiles.isEmpty()) {
            Request.requestError("Запуск в оффлайн-режиме невозможен");
        }

        // Verify launcher signature
        //TODO: TO DIGEST
        //SecurityHelper.verifySign(LegacyLauncherRequest.BINARY_PATH,
        //        settings.lastDigest, Launcher.getConfig().publicKey);

        // Return last sign and profiles
        return new LegacyLauncherRequest.Result(null, settings.lastDigest, settings.lastProfiles);
    }

    @LauncherAPI
    public static void makeJsonRequest(RequestInterface request, Runnable callback) {

    }

    @LauncherAPI
    public static void startTask(@SuppressWarnings("rawtypes") Task task) {
        try {
            worker.queue.put(task);
        } catch (InterruptedException e) {
            LogHelper.error(e);
        }
    }

    @LauncherAPI
    public static HWID getHWID() {
        HWID hhwid = hwid.get();
        if(hhwid == null) hwid.set(hwidProvider.getHWID());
        return hhwid;
    }

    @LauncherAPI
    public static long getTotalMemory() {
        return hwidProvider.getTotalMemory() >> 20;
    }

    @LauncherAPI
    public static HasherStore getDefaultHasherStore() {
        return HasherManager.getDefaultStore();
    }

    @FunctionalInterface
    public interface HashedDirRunnable {
        SignedObjectHolder<HashedDir> run() throws Exception;
    }
}
