package pro.gravit.launcher.client;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.client.events.client.ClientProcessBuilderCreateEvent;
import pro.gravit.launcher.client.events.client.ClientProcessBuilderLaunchedEvent;
import pro.gravit.launcher.client.events.client.ClientProcessBuilderParamsWrittedEvent;
import pro.gravit.launcher.client.events.client.ClientProcessBuilderPreLaunchEvent;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launcher.profiles.optional.OptionalView;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.profiles.optional.actions.OptionalActionClientArgs;
import pro.gravit.launcher.profiles.optional.actions.OptionalActionJvmArgs;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.*;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ClientLauncherProcess {
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
    public Path javaDir;
    public int bits;
    public boolean useLegacyJavaClassPathProperty;
    public boolean isStarted;
    public JavaHelper.JavaVersion javaVersion;
    private transient Process process;

    public ClientLauncherProcess(Path executeFile, Path workDir, Path javaDir, String mainClass) {
        this.executeFile = executeFile;
        this.workDir = workDir;
        this.javaDir = javaDir;
        this.mainClass = mainClass;
    }

    public ClientLauncherProcess(Path clientDir, Path assetDir, Path javaDir,
                                 ClientProfile profile, PlayerProfile playerProfile, String accessToken,
                                 HashedDir clientHDir, HashedDir assetHDir, HashedDir jvmHDir) {
        this(clientDir, assetDir, javaDir, clientDir.resolve("resourcepacks"), profile, playerProfile, null, accessToken, clientHDir, assetHDir, jvmHDir);
    }

    public ClientLauncherProcess(Path clientDir, Path assetDir,
                                 ClientProfile profile, PlayerProfile playerProfile, String accessToken,
                                 HashedDir clientHDir, HashedDir assetHDir, HashedDir jvmHDir) {
        this(clientDir, assetDir, Paths.get(System.getProperty("java.home")), clientDir.resolve("resourcepacks"), profile, playerProfile, null, accessToken, clientHDir, assetHDir, jvmHDir);
    }

    public ClientLauncherProcess(Path clientDir, Path assetDir, Path javaDir, Path resourcePackDir,
                                 ClientProfile profile, PlayerProfile playerProfile, OptionalView view, String accessToken,
                                 HashedDir clientHDir, HashedDir assetHDir, HashedDir jvmHDir) {
        this.workDir = clientDir.toAbsolutePath();
        this.javaDir = javaDir;
        this.executeFile = IOHelper.resolveJavaBin(this.javaDir);
        this.mainClass = ClientLauncherEntryPoint.class.getName();
        this.params.clientDir = this.workDir.toString();
        this.params.resourcePackDir = resourcePackDir.toAbsolutePath().toString();
        this.params.assetDir = assetDir.toAbsolutePath().toString();
        this.params.profile = profile;
        this.params.playerProfile = playerProfile;
        this.params.accessToken = accessToken;
        this.params.assetHDir = assetHDir;
        this.params.clientHDir = clientHDir;
        this.params.javaHDir = jvmHDir;
        if (view != null) {
            this.params.actions = view.getEnabledActions();
        }
        try {
            javaVersion = JavaHelper.JavaVersion.getByPath(javaDir);
        } catch (IOException e) {
            LogHelper.error(e);
            javaVersion = null;
        }
        if (javaVersion == null) {
            javaVersion = JavaHelper.JavaVersion.getCurrentJavaVersion();
        }
        this.bits = JVMHelper.JVM_BITS;
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
        this.systemEnv.put("JAVA_HOME", javaDir.toString());
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
        if (this.params.profile.getRuntimeInClientConfig() != ClientProfile.RuntimeInClientConfig.NONE) {
            jvmModules.add("javafx.base");
            jvmModules.add("javafx.graphics");
            jvmModules.add("javafx.fxml");
            jvmModules.add("javafx.controls");
            jvmModules.add("javafx.swing");
            jvmModules.add("javafx.media");
            jvmModules.add("javafx.web");
        }

        LauncherEngine.modulesManager.invokeEvent(new ClientProcessBuilderCreateEvent(this));
    }

    public void start(boolean pipeOutput) throws IOException, InterruptedException {
        if (isStarted) throw new IllegalStateException("Process already started");
        if (LauncherEngine.guard != null) LauncherEngine.guard.applyGuardParams(this);
        LauncherEngine.modulesManager.invokeEvent(new ClientProcessBuilderPreLaunchEvent(this));
        List<String> processArgs = new LinkedList<>();
        processArgs.add(executeFile.toString());
        processArgs.addAll(jvmArgs);
        if (javaVersion.version >= 9) {
            applyJava9Params(processArgs);
        }
        //ADD CLASSPATH
        if (params.profile.getClassLoaderConfig() == ClientProfile.ClassLoaderConfig.AGENT) {
            processArgs.add("-javaagent:".concat(IOHelper.getCodeSource(ClientLauncherEntryPoint.class).toAbsolutePath().toString()));
        } else if (params.profile.getClassLoaderConfig() == ClientProfile.ClassLoaderConfig.SYSTEM_ARGS) {
            systemClassPath.addAll(ClientLauncherEntryPoint.resolveClassPath(workDir, params.actions, params.profile).map(Path::toString).collect(Collectors.toList()));
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
        processBuilder.environment().put("JAVA_HOME", javaDir.toAbsolutePath().toString());
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
        /*jvmModulesPaths.add(javaVersion.jvmDir);
        jvmModulesPaths.add(javaVersion.jvmDir.resolve("jre"));
        Path openjfxPath = JavaHelper.tryGetOpenJFXPath(javaVersion.jvmDir);
        if (openjfxPath != null) {
            jvmModulesPaths.add(openjfxPath);
        }*/ // TODO: fix runtime in client
        StringBuilder modulesPath = new StringBuilder();
        StringBuilder modulesAdd = new StringBuilder();
        for (String moduleName : jvmModules) {
            /*boolean success = JavaHelper.tryAddModule(jvmModulesPaths, moduleName, modulesPath);
            if (success) {
                if (modulesAdd.length() > 0) modulesAdd.append(",");
                modulesAdd.append(moduleName);
            }*/
            if (modulesAdd.length() > 0) modulesAdd.append(",");
            modulesAdd.append(moduleName);
        }
        for(String modulePath : jvmModulesPaths) {
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
            try (HOutput output = new HOutput(socket.getOutputStream())) {
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
            }
        }
        LauncherEngine.modulesManager.invokeEvent(new ClientProcessBuilderParamsWrittedEvent(this));
    }

    public Process getProcess() {
        return process;
    }

    public static class ClientParams {
        public String assetDir;

        public String clientDir;

        public String resourcePackDir;

        // Client params

        public PlayerProfile playerProfile;

        public ClientProfile profile;

        public String accessToken;

        //==Minecraft params==

        public boolean autoEnter;

        public boolean fullScreen;

        public int ram;

        public int width;

        public int height;

        public Set<OptionalAction> actions = new HashSet<>();

        //========

        public UUID session;

        public AuthRequestEvent.OAuthRequestEvent oauth;

        public String authId;

        public long oauthExpiredTime;

        public Map<String, String> extendedTokens;

        public boolean offlineMode;

        public transient HashedDir assetHDir;

        public transient HashedDir clientHDir;

        public transient HashedDir javaHDir;

        public void addClientArgs(Collection<String> args) {
            if (profile.getVersion().compareTo(ClientProfile.Version.MC164) >= 0)
                addModernClientArgs(args);
            else
                addClientLegacyArgs(args);
        }

        public void addClientLegacyArgs(Collection<String> args) {
            args.add(playerProfile.username);
            args.add(accessToken);

            // Add args for tweaker
            Collections.addAll(args, "--version", profile.getVersion().name);
            Collections.addAll(args, "--gameDir", clientDir);
            Collections.addAll(args, "--assetsDir", assetDir);
        }

        private void addModernClientArgs(Collection<String> args) {

            // Add version-dependent args
            ClientProfile.Version version = profile.getVersion();
            Collections.addAll(args, "--username", playerProfile.username);
            if (version.compareTo(ClientProfile.Version.MC172) >= 0) {
                Collections.addAll(args, "--uuid", Launcher.toHash(playerProfile.uuid));
                Collections.addAll(args, "--accessToken", accessToken);

                // Add 1.7.10+ args (user properties, asset index)
                if (version.compareTo(ClientProfile.Version.MC1710) >= 0) {
                    // Add user properties
                    Collections.addAll(args, "--userType", "mojang");
                    Collections.addAll(args, "--userProperties", "{}");

                    // Add asset index
                    Collections.addAll(args, "--assetIndex", profile.getAssetIndex());
                }
            } else
                Collections.addAll(args, "--session", accessToken);

            // Add version and dirs args
            Collections.addAll(args, "--version", profile.getVersion().name);
            Collections.addAll(args, "--gameDir", clientDir);
            Collections.addAll(args, "--assetsDir", assetDir);
            Collections.addAll(args, "--resourcePackDir", resourcePackDir);
            if (version.compareTo(ClientProfile.Version.MC194) >= 0)
                Collections.addAll(args, "--versionType", "Launcher v" + Version.getVersion().getVersionString());

            // Add server args
            if (autoEnter) {
                Collections.addAll(args, "--server", profile.getServerAddress());
                Collections.addAll(args, "--port", Integer.toString(profile.getServerPort()));
            }
            for (OptionalAction a : actions) {
                if (a instanceof OptionalActionClientArgs) {
                    args.addAll(((OptionalActionClientArgs) a).args);
                }
            }
            // Add window size args
            if (fullScreen)
                Collections.addAll(args, "--fullscreen", Boolean.toString(true));
            if (width > 0 && height > 0) {
                Collections.addAll(args, "--width", Integer.toString(width));
                Collections.addAll(args, "--height", Integer.toString(height));
            }
        }
    }
}
