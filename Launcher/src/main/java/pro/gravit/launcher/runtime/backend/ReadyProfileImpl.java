package pro.gravit.launcher.runtime.backend;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.launcher.base.profiles.PlayerProfile;
import pro.gravit.launcher.core.api.LauncherAPIHolder;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;
import pro.gravit.launcher.runtime.client.ClientLauncherProcess;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class ReadyProfileImpl implements LauncherBackendAPI.ReadyProfile {
    private LauncherBackendImpl backend;
    private ClientProfile profile;
    private ProfileSettingsImpl settings;
    private ClientDownloadImpl.DownloadedDir clientDir;
    private ClientDownloadImpl.DownloadedDir assetDir;
    private ClientDownloadImpl.DownloadedDir javaDir;
    private volatile Thread writeParamsThread;
    private volatile Thread runThread;
    private volatile ClientLauncherProcess process;
    private volatile Process nativeProcess;
    private volatile LauncherBackendAPI.RunCallback callback;

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
        writeParamsThread = new Thread(this::writeParams);
        writeParamsThread.setDaemon(true);
        writeParamsThread.start();
        runThread = new Thread(this::readThread);
        runThread.setDaemon(true);
        runThread.start();
    }

    private void readThread() {
        try {
            process.start(true);
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
        } catch (Exception e) {
            if(e instanceof InterruptedException) {
                return;
            }
            terminate();
        }
    }

    public void terminate() {
        if(nativeProcess == null) {
            return;
        }
        nativeProcess.destroyForcibly();
    }

    public boolean isAlive() {
        return nativeProcess != null && nativeProcess.isAlive();
    }

    private void writeParams() {
        try {
            process.runWriteParams(new InetSocketAddress("127.0.0.1", Launcher.getConfig().clientPort));
        } catch (Throwable e) {
            terminate();
        }
    }
}
