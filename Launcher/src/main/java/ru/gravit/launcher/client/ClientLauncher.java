package ru.gravit.launcher.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.gravit.launcher.*;
import ru.gravit.launcher.guard.LauncherGuardManager;
import ru.gravit.launcher.gui.JSRuntimeProvider;
import ru.gravit.launcher.hasher.DirWatcher;
import ru.gravit.launcher.hasher.FileNameMatcher;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.auth.RestoreSessionRequest;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.stream.StreamObject;
import ru.gravit.utils.PublicURLClassLoader;
import ru.gravit.utils.helper.*;
import ru.gravit.utils.helper.JVMHelper.OS;

import javax.swing.*;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

public final class ClientLauncher {
    private static Gson gson = new Gson();

    private static final class ClassPathFileVisitor extends SimpleFileVisitor<Path> {
        private final Collection<Path> result;

        private ClassPathFileVisitor(Collection<Path> result) {
            this.result = result;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (IOHelper.hasExtension(file, "jar") || IOHelper.hasExtension(file, "zip"))
                result.add(file);
            return super.visitFile(file, attrs);
        }
    }

    public static final class Params extends StreamObject {
        // Client paths
        @LauncherAPI
        public final Path assetDir;
        @LauncherAPI
        public final Path clientDir;

        // Client params
        @LauncherAPI
        public final PlayerProfile pp;
        @LauncherAPI
        public final String accessToken;
        @LauncherAPI
        public final boolean autoEnter;
        @LauncherAPI
        public final boolean fullScreen;
        @LauncherAPI
        public final int ram;
        @LauncherAPI
        public final int width;
        @LauncherAPI
        public final int height;
        @LauncherAPI
        public final long session;

        @LauncherAPI
        public Params(byte[] launcherDigest, Path assetDir, Path clientDir, PlayerProfile pp, String accessToken,
                      boolean autoEnter, boolean fullScreen, int ram, int width, int height) {
            // Client paths
            this.assetDir = assetDir;
            this.clientDir = clientDir;
            // Client params
            this.pp = pp;
            this.accessToken = SecurityHelper.verifyToken(accessToken);
            this.autoEnter = autoEnter;
            this.fullScreen = fullScreen;
            this.ram = ram;
            this.width = width;
            this.height = height;
            this.session = Request.getSession();
        }

        @LauncherAPI
        public Params(HInput input) throws Exception {
            session = input.readLong();
            // Client paths
            assetDir = IOHelper.toPath(input.readString(0));
            clientDir = IOHelper.toPath(input.readString(0));
            // Client params
            pp = new PlayerProfile(input);
            byte[] encryptedAccessToken = input.readByteArray(SecurityHelper.CRYPTO_MAX_LENGTH);
            String accessTokenD = new String(SecurityHelper.decrypt(Launcher.getConfig().secretKeyClient.getBytes(), encryptedAccessToken));
            accessToken = SecurityHelper.verifyToken(accessTokenD);
            autoEnter = input.readBoolean();
            fullScreen = input.readBoolean();
            ram = input.readVarInt();
            width = input.readVarInt();
            height = input.readVarInt();
        }

        @Override
        public void write(HOutput output) throws IOException {
            output.writeLong(session);
            // Client paths
            output.writeString(assetDir.toString(), 0);
            output.writeString(clientDir.toString(), 0);
            pp.write(output);
            try {
                output.writeByteArray(SecurityHelper.encrypt(Launcher.getConfig().secretKeyClient.getBytes(), accessToken.getBytes()), SecurityHelper.CRYPTO_MAX_LENGTH);
            } catch (Exception e) {
                LogHelper.error(e);
            }
            output.writeBoolean(autoEnter);
            output.writeBoolean(fullScreen);
            output.writeVarInt(ram);
            output.writeVarInt(width);
            output.writeVarInt(height);
        }
    }

    private static final String[] EMPTY_ARRAY = new String[0];
    private static final String SOCKET_HOST = "127.0.0.1";
    private static final int SOCKET_PORT = Launcher.getConfig().clientPort;
    private static final String MAGICAL_INTEL_OPTION = "-XX:HeapDumpPath=ThisTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump";

    private static Path JavaBinPath;
    @SuppressWarnings("unused")
    private static final Set<PosixFilePermission> BIN_POSIX_PERMISSIONS = Collections.unmodifiableSet(EnumSet.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE, // Owner
            PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE, // Group
            PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE // Others
    ));
    // Constants
    private static final Path NATIVES_DIR = IOHelper.toPath("natives");
    private static final Path RESOURCEPACKS_DIR = IOHelper.toPath("resourcepacks");
    private static PublicURLClassLoader classLoader;

    public static class ClientUserProperties {
        @LauncherAPI
        String[] skinURL;
        @LauncherAPI
        String[] skinDigest;
        @LauncherAPI
        String[] cloakURL;
        @LauncherAPI
        String[] cloakDigest;
    }

    public static Path getJavaBinPath() {
        return JavaBinPath;
    }

    private static void addClientArgs(Collection<String> args, ClientProfile profile, Params params) {
        PlayerProfile pp = params.pp;

        // Add version-dependent args
        ClientProfile.Version version = profile.getVersion();
        Collections.addAll(args, "--username", pp.username);
        if (version.compareTo(ClientProfile.Version.MC172) >= 0) {
            Collections.addAll(args, "--uuid", Launcher.toHash(pp.uuid));
            Collections.addAll(args, "--accessToken", params.accessToken);

            // Add 1.7.10+ args (user properties, asset index)
            if (version.compareTo(ClientProfile.Version.MC1710) >= 0) {
                // Add user properties
                Collections.addAll(args, "--userType", "mojang");
                ClientUserProperties properties = new ClientUserProperties();
                if (pp.skin != null) {
                    properties.skinURL = new String[]{pp.skin.url};
                    properties.skinDigest = new String[]{SecurityHelper.toHex(pp.skin.digest)};
                }
                if (pp.cloak != null) {
                    properties.cloakURL = new String[]{pp.cloak.url};
                    properties.cloakDigest = new String[]{SecurityHelper.toHex(pp.cloak.digest)};
                }
                Collections.addAll(args, "--userProperties", ClientLauncher.gson.toJson(properties));

                // Add asset index
                Collections.addAll(args, "--assetIndex", profile.getAssetIndex());
            }
        } else
            Collections.addAll(args, "--session", params.accessToken);

        // Add version and dirs args
        Collections.addAll(args, "--version", profile.getVersion().name);
        Collections.addAll(args, "--gameDir", params.clientDir.toString());
        Collections.addAll(args, "--assetsDir", params.assetDir.toString());
        Collections.addAll(args, "--resourcePackDir", params.clientDir.resolve(RESOURCEPACKS_DIR).toString());
        if (version.compareTo(ClientProfile.Version.MC194) >= 0)
            Collections.addAll(args, "--versionType", "Launcher v" + Launcher.getVersion().getVersionString());

        // Add server args
        if (params.autoEnter) {
            Collections.addAll(args, "--server", profile.getServerAddress());
            Collections.addAll(args, "--port", Integer.toString(profile.getServerPort()));
        }
        profile.pushOptionalClientArgs(args);
        // Add window size args
        if (params.fullScreen)
            Collections.addAll(args, "--fullscreen", Boolean.toString(true));
        if (params.width > 0 && params.height > 0) {
            Collections.addAll(args, "--width", Integer.toString(params.width));
            Collections.addAll(args, "--height", Integer.toString(params.height));
        }
    }

    @LauncherAPI
    public static void setJavaBinPath(Path javaBinPath) {
        JavaBinPath = javaBinPath;
    }

    private static void addClientLegacyArgs(Collection<String> args, ClientProfile profile, Params params) {
        args.add(params.pp.username);
        args.add(params.accessToken);

        // Add args for tweaker
        Collections.addAll(args, "--version", profile.getVersion().name);
        Collections.addAll(args, "--gameDir", params.clientDir.toString());
        Collections.addAll(args, "--assetsDir", params.assetDir.toString());
    }

    @LauncherAPI
    public static void checkJVMBitsAndVersion() {
        if (JVMHelper.JVM_BITS != JVMHelper.OS_BITS) {
            String error = String.format("У Вас установлена Java %d, но Ваша система определена как %d. Установите Java правильной разрядности", JVMHelper.JVM_BITS, JVMHelper.OS_BITS);
            LogHelper.error(error);
            if (Launcher.getConfig().isWarningMissArchJava)
                JOptionPane.showMessageDialog(null, error);
        }
        String jvmVersion = JVMHelper.RUNTIME_MXBEAN.getVmVersion();
        LogHelper.info(jvmVersion);
        if (jvmVersion.startsWith("10.") || jvmVersion.startsWith("9.") || jvmVersion.startsWith("11.")) {
            String error = String.format("У Вас установлена Java %s. Для правильной работы необходима Java 8", JVMHelper.RUNTIME_MXBEAN.getVmVersion());
            LogHelper.error(error);
            if (Launcher.getConfig().isWarningMissArchJava)
                JOptionPane.showMessageDialog(null, error);
        }
    }

    @LauncherAPI
    public static boolean isLaunched() {
        return Launcher.LAUNCHED.get();
    }

    private static void launch(ClientProfile profile, Params params) throws Throwable {
        // Add client args
        Collection<String> args = new LinkedList<>();
        if (profile.getVersion().compareTo(ClientProfile.Version.MC164) >= 0)
            addClientArgs(args, profile, params);
        else
            addClientLegacyArgs(args, profile, params);
        Collections.addAll(args, profile.getClientArgs());
        LogHelper.debug("Args: " + args);
        // Resolve main class and method
        Class<?> mainClass = classLoader.loadClass(profile.getMainClass());
        MethodHandle mainMethod = MethodHandles.publicLookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class));
        // Invoke main method with exception wrapping
        Launcher.LAUNCHED.set(true);
        JVMHelper.fullGC();
        System.setProperty("minecraft.applet.TargetDirectory", params.clientDir.toString()); // For 1.5.2
        mainMethod.invoke((Object) args.toArray(EMPTY_ARRAY));
    }

    private static Process process = null;
    private static boolean clientStarted = false;
    private static Thread writeParamsThread;

    @LauncherAPI
    public static Process launch(
            HashedDir assetHDir, HashedDir clientHDir,
            ClientProfile profile, Params params, boolean pipeOutput) throws Throwable {
        LogHelper.debug("Writing ClientLauncher params");
        ClientLauncherContext context = new ClientLauncherContext();
        clientStarted = false;
        if (writeParamsThread != null && writeParamsThread.isAlive()) {
            writeParamsThread.interrupt();
        }
        writeParamsThread = CommonHelper.newThread("Client params writter", true, () ->
        {
            try {
                try (ServerSocket socket = new ServerSocket()) {

                    socket.setReuseAddress(true);
                    socket.setSoTimeout(30000);
                    socket.bind(new InetSocketAddress(SOCKET_HOST, SOCKET_PORT));
                    Socket client = socket.accept();
                    if (process == null) {
                        LogHelper.error("Process is null");
                        return;
                    }
                    if (!process.isAlive()) {
                        LogHelper.error("Process is not alive");
                        JOptionPane.showMessageDialog(null, "Client Process crashed", "Launcher", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    try (HOutput output = new HOutput(client.getOutputStream())) {
                        params.write(output);
                        output.writeString(Launcher.gson.toJson(profile), 0);
                        assetHDir.write(output);
                        clientHDir.write(output);
                    }
                    clientStarted = true;
                }
            } catch (IOException e) {
                LogHelper.error(e);
            }
        });
        writeParamsThread.start();
        checkJVMBitsAndVersion();
        LogHelper.debug("Resolving JVM binary");
        Path javaBin = LauncherGuardManager.getGuardJavaBinPath();
        context.javaBin = javaBin;
        context.clientProfile = profile;
        context.playerProfile = params.pp;
        context.args.add(javaBin.toString());
        context.args.add(MAGICAL_INTEL_OPTION);
        if (params.ram > 0 && params.ram <= JVMHelper.RAM) {
            context.args.add("-Xms" + params.ram + 'M');
            context.args.add("-Xmx" + params.ram + 'M');
        }
        context.args.add(JVMHelper.jvmProperty(LogHelper.DEBUG_PROPERTY, Boolean.toString(LogHelper.isDebugEnabled())));
        context.args.add(JVMHelper.jvmProperty(LogHelper.STACKTRACE_PROPERTY, Boolean.toString(LogHelper.isStacktraceEnabled())));
        context.args.add(JVMHelper.jvmProperty(LogHelper.DEV_PROPERTY, Boolean.toString(LogHelper.isDevEnabled())));
        JVMHelper.addSystemPropertyToArgs(context.args, DirBridge.CUSTOMDIR_PROPERTY);
        JVMHelper.addSystemPropertyToArgs(context.args, DirBridge.USE_CUSTOMDIR_PROPERTY);
        JVMHelper.addSystemPropertyToArgs(context.args, DirBridge.USE_OPTDIR_PROPERTY);
        if (JVMHelper.OS_TYPE == OS.MUSTDIE) {
            if (JVMHelper.OS_VERSION.startsWith("10.")) {
                LogHelper.debug("MustDie 10 fix is applied");
                context.args.add(JVMHelper.jvmProperty("os.name", "Windows 10"));
                context.args.add(JVMHelper.jvmProperty("os.version", "10.0"));
            }
        }
        // Add classpath and main class
        String pathLauncher = IOHelper.getCodeSource(ClientLauncher.class).toString();
        context.pathLauncher = pathLauncher;
        Collections.addAll(context.args, ClientLauncherWrapper.MAGIC_ARG);
        Collections.addAll(context.args, profile.getJvmArgs());
        profile.pushOptionalJvmArgs(context.args);
        Collections.addAll(context.args, "-Djava.library.path=".concat(params.clientDir.resolve(NATIVES_DIR).toString())); // Add Native Path
        Collections.addAll(context.args, "-javaagent:".concat(pathLauncher));
        LauncherGuardManager.guard.addCustomParams(context);
        Collections.addAll(context.args, ClientLauncher.class.getName());

        // Print commandline debug message
        LogHelper.debug("Commandline: " + context.args);

        // Build client process
        LogHelper.debug("Launching client instance");
        ProcessBuilder builder = new ProcessBuilder(context.args);
        context.builder = builder;
        LauncherGuardManager.guard.addCustomEnv(context);
        //else
        //builder.environment().put("CLASSPATH", classPathString.toString());
        EnvHelper.addEnv(builder);
        builder.directory(params.clientDir.toFile());
        builder.inheritIO();
        if (pipeOutput) {
            builder.redirectErrorStream(true);
            builder.redirectOutput(Redirect.PIPE);
        }
        // Let's rock!
        process = builder.start();
        if (!LogHelper.isDebugEnabled()) {
            for (int i = 0; i < 50; ++i) {
                if (!process.isAlive()) {
                    int exitCode = process.exitValue();
                    LogHelper.error("Process exit code %d", exitCode);
                    if (writeParamsThread != null && writeParamsThread.isAlive()) writeParamsThread.interrupt();
                    break;
                }
                if (clientStarted) {
                    break;
                }
                Thread.sleep(200);
            }
            if (!clientStarted) {
                LogHelper.error("Write Client Params not successful. Using debug mode for more information");
            }
        }
        clientStarted = false;
        return process;
    }

    @LauncherAPI
    public static void main(String... args) throws Throwable {
        LauncherEngine engine = LauncherEngine.clientInstance();
        Launcher.modulesManager = new ClientModuleManager(engine);
        LauncherConfig.getAutogenConfig().initModules(); //INIT
        initGson();
        Launcher.modulesManager.preInitModules();
        JVMHelper.verifySystemProperties(ClientLauncher.class, true);
        EnvHelper.checkDangerousParams();
        JVMHelper.checkStackTrace(ClientLauncher.class);
        LogHelper.printVersion("Client Launcher");
        if (engine.runtimeProvider == null) engine.runtimeProvider = new JSRuntimeProvider();
        engine.runtimeProvider.init(true);
        engine.runtimeProvider.preLoad();
        LauncherGuardManager.initGuard(true);
        LogHelper.debug("Reading ClientLauncher params");
        Params params;
        ClientProfile profile;
        HashedDir assetHDir, clientHDir;
        try {
            try (Socket socket = IOHelper.newSocket()) {
                socket.connect(new InetSocketAddress(SOCKET_HOST, SOCKET_PORT));
                try (HInput input = new HInput(socket.getInputStream())) {
                    params = new Params(input);
                    profile = gson.fromJson(input.readString(0), ClientProfile.class);
                    assetHDir = new HashedDir(input);
                    clientHDir = new HashedDir(input);
                }
            }
        } catch (IOException ex) {
            LogHelper.error(ex);
            System.exit(-98);
            return;
        }
        Launcher.profile = profile;
        Request.setSession(params.session);
        checkJVMBitsAndVersion();
        Launcher.modulesManager.initModules();
        // Verify ClientLauncher sign and classpath
        LogHelper.debug("Verifying ClientLauncher sign and classpath");
        LinkedList<Path> classPath = resolveClassPathList(params.clientDir, profile.getClassPath());
        for (Path classpathURL : classPath) {
            LauncherAgent.addJVMClassPath(classpathURL.toAbsolutePath().toString());
        }
        profile.pushOptionalClassPath(cp -> {
            LinkedList<Path> optionalClassPath = resolveClassPathList(params.clientDir, cp);
            for (Path classpathURL : optionalClassPath) {
                LauncherAgent.addJVMClassPath(classpathURL.toAbsolutePath().toString());
            }
        });
        URL[] classpathurls = resolveClassPath(params.clientDir, profile.getClassPath());
        classLoader = new PublicURLClassLoader(classpathurls, ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        classLoader.nativePath = params.clientDir.resolve(NATIVES_DIR).toString();
        PublicURLClassLoader.systemclassloader = classLoader;
        // Start client with WatchService monitoring
        boolean digest = !profile.isUpdateFastCheck();
        LogHelper.debug("Restore sessions");
        RestoreSessionRequest request = new RestoreSessionRequest(Request.getSession());
        request.request();
        LegacyRequestBridge.service.reconnectCallback = () ->
        {
            LogHelper.debug("WebSocket connect closed. Try reconnect");
            try {
                if (!LegacyRequestBridge.service.reconnectBlocking()) LogHelper.error("Error connecting");
                LogHelper.debug("Connect to %s", Launcher.getConfig().address);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                RestoreSessionRequest request1 = new RestoreSessionRequest(Request.getSession());
                request1.request();
            } catch (Exception e) {
                LogHelper.error(e);
            }
        };
        LogHelper.debug("Starting JVM and client WatchService");
        FileNameMatcher assetMatcher = profile.getAssetUpdateMatcher();
        FileNameMatcher clientMatcher = profile.getClientUpdateMatcher();
        try (DirWatcher assetWatcher = new DirWatcher(params.assetDir, assetHDir, assetMatcher, digest);
             DirWatcher clientWatcher = new DirWatcher(params.clientDir, clientHDir, clientMatcher, digest)) {
            // Verify current state of all dirs
            //verifyHDir(IOHelper.JVM_DIR, jvmHDir.object, null, digest);
            //for (OptionalFile s : Launcher.profile.getOptional()) {
            //    if (params.updateOptional.contains(s)) s.mark = true;
            //    else hdir.removeR(s.file);
            //}
            Launcher.profile.pushOptionalFile(clientHDir, false);
            Launcher.modulesManager.postInitModules();
            // Start WatchService, and only then client
            CommonHelper.newThread("Asset Directory Watcher", true, assetWatcher).start();
            CommonHelper.newThread("Client Directory Watcher", true, clientWatcher).start();
            verifyHDir(params.assetDir, assetHDir, assetMatcher, digest);
            verifyHDir(params.clientDir, clientHDir, clientMatcher, digest);
            launch(profile, params);
        }
    }

    private static URL[] resolveClassPath(Path clientDir, String... classPath) throws IOException {
        Collection<Path> result = new LinkedList<>();
        for (String classPathEntry : classPath) {
            Path path = clientDir.resolve(IOHelper.toPath(classPathEntry));
            if (IOHelper.isDir(path)) { // Recursive walking and adding
                IOHelper.walk(path, new ClassPathFileVisitor(result), false);
                continue;
            }
            result.add(path);
        }
        return result.stream().map(IOHelper::toURL).toArray(URL[]::new);
    }

    private static LinkedList<Path> resolveClassPathList(Path clientDir, String... classPath) throws IOException {
        LinkedList<Path> result = new LinkedList<>();
        for (String classPathEntry : classPath) {
            Path path = clientDir.resolve(IOHelper.toPath(classPathEntry));
            if (IOHelper.isDir(path)) { // Recursive walking and adding
                IOHelper.walk(path, new ClassPathFileVisitor(result), false);
                continue;
            }
            result.add(path);
        }
        return result;
    }

    public static void initGson() {
        if (Launcher.gson != null) return;
        Launcher.gsonBuilder = new GsonBuilder();
        Launcher.gson = Launcher.gsonBuilder.create();
    }

    @LauncherAPI
    public static void setProfile(ClientProfile profile) {
        Launcher.profile = profile;
        LogHelper.debug("New Profile name: %s", profile.getTitle());
    }

    public static void verifyHDir(Path dir, HashedDir hdir, FileNameMatcher matcher, boolean digest) throws IOException {
        if (matcher != null)
            matcher = matcher.verifyOnly();

        // Hash directory and compare (ignore update-only matcher entries, it will break offline-mode)
        HashedDir currentHDir = new HashedDir(dir, matcher, true, digest);
        if (!hdir.diff(currentHDir, matcher).isSame())
            throw new SecurityException(String.format("Forbidden modification: '%s'", IOHelper.getFileName(dir)));
    }

    private ClientLauncher() {
    }
}
