package pro.gravit.launcher.runtime.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.launcher.base.profiles.PlayerProfile;
import pro.gravit.launcher.client.events.ClientProcessLaunchEvent;
import pro.gravit.launcher.client.utils.DirWatcher;
import pro.gravit.launcher.client.utils.MinecraftAuthlibBridge;
import pro.gravit.launcher.core.api.LauncherAPIHolder;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;
import pro.gravit.launcher.core.hasher.FileNameMatcher;
import pro.gravit.launcher.runtime.client.ClientLauncherProcess;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import static pro.gravit.launcher.client.ClientLauncherEntryPoint.verifyHDir;

public class ReadyProfileImpl implements LauncherBackendAPI.ReadyProfile {

    private static final Logger logger =
            LoggerFactory.getLogger(ReadyProfileImpl.class);

    final LauncherBackendImpl backend;
    ClientProfile profile;
    final ProfileSettingsImpl settings;
    final ClientDownloadImpl.DownloadedDir clientDir;
    final ClientDownloadImpl.DownloadedDir assetDir;
    final ClientDownloadImpl.DownloadedDir javaDir;
    private volatile Thread writeParamsThread;
    private volatile Thread runThread;
    volatile ClientLauncherProcess process;
    volatile Process nativeProcess;
    volatile LauncherBackendAPI.RunCallback callback;
    volatile MinecraftAuthlibBridge bridge;
    private volatile DirWatcher assetWatcher;
    private volatile DirWatcher clientWatcher;
    private volatile DirWatcher javaWatcher;

    public ReadyProfileImpl(LauncherBackendImpl backend, ClientProfile profile, ProfileSettingsImpl settings, ClientDownloadImpl.DownloadedDir clientDir, ClientDownloadImpl.DownloadedDir assetDir, ClientDownloadImpl.DownloadedDir javaDir) {
        this.backend = backend;
        this.profile = profile;
        this.settings = settings;
        this.clientDir = clientDir;
        this.assetDir = assetDir;
        this.javaDir = javaDir;
    }

    @Override
    public ProfileFeatureAPI.ClientProfile getClientProfile() {
        return profile;
    }

    @Override
    public LauncherBackendAPI.ClientProfileSettings getSettings() {
        return settings;
    }

    @Override
    public void run(LauncherBackendAPI.RunCallback callback) {
        if(isAlive()) {
            terminate();
        }
        this.callback = callback;
        if(backend.hasPermission("launcher.debug.skipfilemonitor") && settings.hasFlag(LauncherBackendAPI.ClientProfileSettings.Flag.DEBUG_SKIP_FILE_MONITOR)) {
            var builder = new ClientProfileBuilder(profile);
            builder.setUpdate(new ArrayList<>());
            builder.setUpdateVerify(new ArrayList<>());
            builder.setUpdateExclusions(new ArrayList<>());
            profile = builder.createClientProfile();
        }
        var java = settings.getSelectedJava();
        if(java == null) {
            java = settings.getRecommendedJava();
        }
        process = new ClientLauncherProcess(clientDir.path(), assetDir.path(), java, clientDir.path().resolve("resourcepacks"),
                profile, new PlayerProfile(backend.getSelfUser()), settings.view, backend.getSelfUser().getAccessToken(),
                clientDir.dir(), assetDir.dir(), javaDir == null ? null : javaDir.dir(),
                new AuthRequestEvent.OAuthRequestEvent(backend.getAuthToken()), backend.getAuthMethod().getName());
        process.params.ram = (int) (settings.getReservedMemoryBytes(LauncherBackendAPI.ClientProfileSettings.MemoryClass.TOTAL) >> 20);
        if (process.params.ram > 0) {
            process.jvmArgs.add("-Xms" + process.params.ram + 'M');
            process.jvmArgs.add("-Xmx" + process.params.ram + 'M');
        }
        process.params.fullScreen = settings.hasFlag(LauncherBackendAPI.ClientProfileSettings.Flag.FULLSCREEN);
        process.params.autoEnter = settings.hasFlag(LauncherBackendAPI.ClientProfileSettings.Flag.AUTO_ENTER);
        if(JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            process.params.lwjglGlfwWayland = settings.hasFlag(LauncherBackendAPI.ClientProfileSettings.Flag.LINUX_WAYLAND_SUPPORT);
        }
        if(process.params.profile.getClassLoaderConfig() == ClientProfile.ClassLoaderConfig.BRIDGE) {
            writeParamsThread = new Thread(this::authlibServer);
            writeParamsThread.setDaemon(true);
            writeParamsThread.start();
        } else {
            writeParamsThread = new Thread(this::writeParams);
            writeParamsThread.setDaemon(true);
            writeParamsThread.start();
        }
        runThread = new Thread(this::readThread);
        runThread.setDaemon(true);
        runThread.start();
    }

    private void readThread() {
        Runnable onClose = null;
        try {
            if(process.params.profile.getClassLoaderConfig() == ClientProfile.ClassLoaderConfig.BRIDGE) {
                logger.debug("Start watchers for {}", profile.getTitle());
                FileNameMatcher assetMatcher = profile.getAssetUpdateMatcher();
                FileNameMatcher clientMatcher = profile.getClientUpdateMatcher();
                DirWatcher assetWatcher = new BridgeDirWatcher(assetDir.path(), process.params.assetHDir, assetMatcher, true, this);
                DirWatcher clientWatcher = new BridgeDirWatcher(clientDir.path(), process.params.clientHDir, clientMatcher, true, this);
                DirWatcher javaWatcher = process.params.javaHDir == null ? null : new BridgeDirWatcher(javaDir.path(), process.params.javaHDir, null, true, this);
                onClose = () -> {
                    try {
                        assetWatcher.close();
                        clientWatcher.close();
                        if(javaWatcher != null) {
                            javaWatcher.close();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };
                CommonHelper.newThread("Asset Directory Watcher", true, assetWatcher).start();
                CommonHelper.newThread("Client Directory Watcher", true, clientWatcher).start();
                if (javaWatcher != null)
                    CommonHelper.newThread("Java Directory Watcher", true, javaWatcher).start();
                verifyHDir(assetDir.path(), process.params.assetHDir, assetMatcher, false, false);
                verifyHDir(clientDir.path(), process.params.clientHDir, clientMatcher, false, true);
                if (javaWatcher != null)
                    verifyHDir(javaDir.path(), process.params.javaHDir, null, false, true);
            }
            logger.debug("Start client {}", profile.getTitle());
            process.start(true);
            logger.debug("Start watching client IO {}", profile.getTitle());
            readIOLoop();
            callback.onReadyToExit();
        } catch (Throwable e) {
            if(e instanceof InterruptedException) {
                return;
            }
            logger.error("", e);
            terminate();
        } finally {
            if(onClose != null) {
                onClose.run();
            }
            if(bridge != null) {
                bridge.close();
            }
        }
    }

    private void readIOLoop() throws IOException, InterruptedException {
        nativeProcess = process.getProcess();
        callback.onCanTerminate(this::terminate);
        InputStream stream = nativeProcess.getInputStream();
        byte[] buf = IOHelper.newBuffer();
        try {
            for (int length = stream.read(buf); length >= 0; length = stream.read(buf)) {
                callback.onNormalOutput(buf, 0, length);
            }
        } catch (EOFException ignored) {
        }
        if (nativeProcess.isAlive()) {
            int code = nativeProcess.waitFor();
            callback.onFinished(code);
        }
    }

    public void terminate() {
        try {
            if(assetWatcher == null) {
                assetWatcher.close();
            }
            if(clientWatcher == null) {
                clientWatcher.close();
            }
            if(javaWatcher != null) {
                javaWatcher.close();
            }
        } catch (Throwable e) {
            logger.error("Error when close watchers", e);
        }
        if(bridge != null) {
            bridge.close();
        }
        if(nativeProcess == null) {
            return;
        }
        nativeProcess.destroyForcibly();
        callback.onReadyToExit();
    }

    public boolean isAlive() {
        return nativeProcess != null && nativeProcess.isAlive();
    }

    private void writeParams() {
        try {
            process.runWriteParams(new InetSocketAddress("127.0.0.1", Launcher.getConfig().clientPort));
            callback.onReadyToExit();
        } catch (Throwable e) {
            terminate();
        }
    }

    private void authlibServer() {
        try {
            bridge = process.runAuthlibBridgeServer(new InetSocketAddress("127.0.0.1", Launcher.getConfig().clientPort));
        } catch (Throwable e) {
            terminate();
        }
    }
}