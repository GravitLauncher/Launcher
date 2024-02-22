package pro.gravit.launcher.client;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.client.events.client.ClientProcessBuilderCreateEvent;
import pro.gravit.launcher.client.events.client.ClientProcessBuilderLaunchedEvent;
import pro.gravit.launcher.client.events.client.ClientProcessBuilderParamsWrittedEvent;
import pro.gravit.launcher.client.events.client.ClientProcessBuilderPreLaunchEvent;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launcher.profiles.optional.OptionalView;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.profiles.optional.actions.OptionalActionJvmArgs;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.utils.helper.*;

import javax.crypto.CipherOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ClientLauncherProcess {
    public final List<String> pre = new LinkedList<>();
    public final ClientParams params = new ClientParams();
    public final List<String> jvmArgs = new LinkedList<>();
    public final List<String> jvmModules = new LinkedList<>();
    public final List<String> jvmModulesPaths = new LinkedList<>();
    public final List<String> systemClientArgs = new LinkedList<>();
    public final List<String> systemClassPath = new LinkedList<>();
    public final Map<String, String> systemEnv = new HashMap<>();
    public final String mainClass;
    private final transient Boolean[] waitWriteParams = new Boolean[]{false};
    public Path executeFile;
    public Path workDir;
    public JavaHelper.JavaVersion javaVersion;
    public boolean useLegacyJavaClassPathProperty;
    public boolean isStarted;
    private transient Process process;

    public ClientLauncherProcess(Path executeFile, Path workDir, JavaHelper.JavaVersion javaVersion, String mainClass) {
        this.executeFile = executeFile;
        this.workDir = workDir;
        this.javaVersion = javaVersion;
        this.mainClass = mainClass;
    }

    public ClientLauncherProcess(Path clientDir, Path assetDir, JavaHelper.JavaVersion javaVersion,
                                 ClientProfile profile, PlayerProfile playerProfile, String accessToken,
                                 HashedDir clientHDir, HashedDir assetHDir, HashedDir jvmHDir) {
        this(clientDir, assetDir, javaVersion, clientDir.resolve("resourcepacks"), profile, playerProfile, null, accessToken, clientHDir, assetHDir, jvmHDir);
    }

    public ClientLauncherProcess(Path clientDir, Path assetDir,
                                 ClientProfile profile, PlayerProfile playerProfile, String accessToken,
                                 HashedDir clientHDir, HashedDir assetHDir, HashedDir jvmHDir) {
        this(clientDir, assetDir, JavaHelper.JavaVersion.getCurrentJavaVersion(), clientDir.resolve("resourcepacks"), profile, playerProfile, null, accessToken, clientHDir, assetHDir, jvmHDir);
    }

    public ClientLauncherProcess(Path clientDir, Path assetDir, JavaHelper.JavaVersion javaVersion, Path resourcePackDir,
                                 ClientProfile profile, PlayerProfile playerProfile, OptionalView view, String accessToken,
                                 HashedDir clientHDir, HashedDir assetHDir, HashedDir jvmHDir) {
        this.javaVersion = javaVersion;
        this.workDir = clientDir.toAbsolutePath();
        this.executeFile = IOHelper.resolveJavaBin(this.javaVersion.jvmDir);
        this.mainClass = ClientLauncherEntryPoint.class.getName();
        this.params.clientDir = this.workDir.toString();
        this.params.resourcePackDir = resourcePackDir.toAbsolutePath().toString();
        this.params.assetDir = assetDir.toAbsolutePath().toString();
        this.params.timestamp = System.currentTimeMillis();
        Path nativesPath;
        if(profile.hasFlag(ClientProfile.CompatibilityFlags.LEGACY_NATIVES_DIR)) {
            nativesPath = workDir.resolve("natives");
        } else {
            nativesPath = workDir.resolve("natives").resolve(JVMHelper.OS_TYPE.name).resolve(javaVersion.arch.name);
        }
        if (!Files.isDirectory(nativesPath)) {
            throw new RuntimeException(String.format("Natives dir %s not exist! Your operating system or architecture not supported", nativesPath.toAbsolutePath()));
        }
        this.params.nativesDir = nativesPath.toString();
        this.params.profile = profile;
        this.params.playerProfile = playerProfile;
        this.params.accessToken = accessToken;
        this.params.assetHDir = assetHDir;
        this.params.clientHDir = clientHDir;
        this.params.javaHDir = jvmHDir;
        if (view != null) {
            this.params.actions = view.getEnabledActions();
        }
        applyClientProfile();
    }

    public static String getPathSeparator() {
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE)
            return ";";
        else
            return ":";
    }

    private void applyClientProfile() {
        this.systemClassPath.add(IOHelper.getCodeSource(ClientLauncherEntryPoint.class).toAbsolutePath().toString());
        Collections.addAll(this.jvmArgs, this.params.profile.getJvmArgs());
        for (OptionalAction a : this.params.actions) {
            if (a instanceof OptionalActionJvmArgs) {
                this.jvmArgs.addAll(((OptionalActionJvmArgs) a).args);
            }
        }
        this.systemEnv.put("JAVA_HOME", javaVersion.jvmDir.toString());
        Collections.addAll(this.systemClassPath, this.params.profile.getAlternativeClassPath());
        if (params.ram > 0) {
            this.jvmArgs.add("-Xmx" + params.ram + 'M');
        }
        this.params.oauth = Request.getOAuth();
        if (this.params.oauth == null) {
            throw new UnsupportedOperationException("Legacy session not supported");
        } else {
            this.params.authId = Request.getAuthId();
            this.params.oauthExpiredTime = Request.getTokenExpiredTime();
            this.params.extendedTokens = Request.getExtendedTokens();
        }
        this.jvmModules.addAll(this.params.profile.getModules());
        this.jvmModulesPaths.addAll(this.params.profile.getModulePath());
        LauncherEngine.modulesManager.invokeEvent(new ClientProcessBuilderCreateEvent(this));
    }

    public void start(boolean pipeOutput) throws IOException, InterruptedException {
        if (isStarted) throw new IllegalStateException("Process already started");
        LauncherEngine.modulesManager.invokeEvent(new ClientProcessBuilderPreLaunchEvent(this));
        List<String> processArgs = new LinkedList<>(pre);
        processArgs.add(executeFile.toString());
        processArgs.addAll(jvmArgs);
        if (javaVersion.version >= 9) {
            applyJava9Params(processArgs);
        }
        //ADD CLASSPATH
        processArgs.add(JVMHelper.jvmProperty("java.library.path", this.params.nativesDir));
        if (params.profile.getClassLoaderConfig() == ClientProfile.ClassLoaderConfig.AGENT) {
            processArgs.add("-javaagent:".concat(IOHelper.getCodeSource(ClientLauncherEntryPoint.class).toAbsolutePath().toString()));
        } else if (params.profile.getClassLoaderConfig() == ClientProfile.ClassLoaderConfig.SYSTEM_ARGS) {
            systemClassPath.addAll(ClientLauncherEntryPoint.resolveClassPath(workDir, params.actions, params.profile)
                    .filter(x -> !params.profile.getModulePath().contains(workDir.relativize(x).toString()))
                    .map(Path::toString)
                    .collect(Collectors.toList()));
        }
        if (Launcher.getConfig().environment != LauncherConfig.LauncherEnvironment.PROD) {
            processArgs.add(JVMHelper.jvmProperty(LogHelper.DEV_PROPERTY, String.valueOf(LogHelper.isDevEnabled())));
            processArgs.add(JVMHelper.jvmProperty(LogHelper.DEBUG_PROPERTY, String.valueOf(LogHelper.isDebugEnabled())));
            processArgs.add(JVMHelper.jvmProperty(LogHelper.STACKTRACE_PROPERTY, String.valueOf(LogHelper.isStacktraceEnabled())));
        }
        if (useLegacyJavaClassPathProperty) {
            processArgs.add("-Djava.class.path=".concat(String.join(getPathSeparator(), systemClassPath)));
        } else {
            processArgs.add("-cp");
            processArgs.add(String.join(getPathSeparator(), systemClassPath));
        }
        processArgs.add(mainClass);
        processArgs.addAll(systemClientArgs);
        synchronized (waitWriteParams) {
            if (!waitWriteParams[0]) {
                waitWriteParams.wait(1000);
            }
        }
        if (LogHelper.isDebugEnabled())
            LogHelper.debug("Commandline: %s", Arrays.toString(processArgs.toArray()));
        ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
        EnvHelper.addEnv(processBuilder);
        if(JVMHelper.OS_TYPE == JVMHelper.OS.LINUX){
            var env = processBuilder.environment();
            // https://github.com/Admicos/minecraft-wayland/issues/55
            env.put("__GL_THREADED_OPTIMIZATIONS", "0");
            if(params.lwjglGlfwWayland) {
                env.remove("DISPLAY"); // No X11
            }
        }
        processBuilder.environment().put("JAVA_HOME", javaVersion.jvmDir.toAbsolutePath().toString());
        processBuilder.environment().putAll(systemEnv);
        processBuilder.directory(workDir.toFile());
        processBuilder.inheritIO();
        if (pipeOutput) {
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        }
        process = processBuilder.start();
        LauncherEngine.modulesManager.invokeEvent(new ClientProcessBuilderLaunchedEvent(this));
        isStarted = true;
    }

    private void applyJava9Params(List<String> processArgs) {
        // TODO: fix runtime in client
        StringBuilder modulesPath = new StringBuilder();
        StringBuilder modulesAdd = new StringBuilder();
        for (String moduleName : jvmModules) {
            if (modulesAdd.length() > 0) modulesAdd.append(",");
            modulesAdd.append(moduleName);
        }
        for (String modulePath : jvmModulesPaths) {
            if (modulesPath.length() > 0) modulesPath.append(File.pathSeparator);
            modulesPath.append(modulePath);
        }
        if (modulesAdd.length() > 0) {
            processArgs.add("--add-modules");
            processArgs.add(modulesAdd.toString());
        }
        if (modulesPath.length() > 0) {
            processArgs.add("--module-path");
            processArgs.add(modulesPath.toString());
        }
    }

    public void runWriteParams(SocketAddress address) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(address);
            synchronized (waitWriteParams) {
                waitWriteParams[0] = true;
                waitWriteParams.notifyAll();
            }
            Socket socket = serverSocket.accept();
            try (HOutput output = new HOutput(new CipherOutputStream(socket.getOutputStream(), SecurityHelper.newAESEncryptCipher(SecurityHelper.fromHex(Launcher.getConfig().secretKeyClient))))) {
                byte[] serializedMainParams = IOHelper.encode(Launcher.gsonManager.gson.toJson(params));
                output.writeByteArray(serializedMainParams, 0);
                params.clientHDir.write(output);
                params.assetHDir.write(output);
                if (params.javaHDir == null || params.javaHDir == params.assetHDir) { //OLD RUNTIME USE params.assetHDir AS NULL IN java.javaHDir
                    output.writeBoolean(false);
                } else {
                    output.writeBoolean(true);
                    params.javaHDir.write(output);
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        LauncherEngine.modulesManager.invokeEvent(new ClientProcessBuilderParamsWrittedEvent(this));
    }

    public Process getProcess() {
        return process;
    }

}
