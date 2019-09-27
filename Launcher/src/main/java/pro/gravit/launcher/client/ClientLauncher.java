package pro.gravit.launcher.client;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

import pro.gravit.launcher.ClientLauncherWrapper;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.LauncherAgent;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.api.AuthService;
import pro.gravit.launcher.api.ClientService;
import pro.gravit.launcher.client.events.ClientLaunchPhase;
import pro.gravit.launcher.client.events.ClientLauncherInitPhase;
import pro.gravit.launcher.client.events.ClientLauncherPostInitPhase;
import pro.gravit.launcher.guard.LauncherGuardManager;
import pro.gravit.launcher.gui.JSRuntimeProvider;
import pro.gravit.launcher.hasher.FileNameMatcher;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.hwid.HWIDProvider;
import pro.gravit.launcher.managers.ClientGsonManager;
import pro.gravit.launcher.managers.ClientHookManager;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.auth.RestoreSessionRequest;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launcher.serialize.stream.StreamObject;
import pro.gravit.launcher.utils.DirWatcher;
import pro.gravit.launcher.utils.NativeJVMHalt;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.EnvHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.JVMHelper.OS;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

public final class ClientLauncher {
    private static final class ClassPathFileVisitor extends SimpleFileVisitor<Path> {
        private final Stream.Builder<Path> result;

        private ClassPathFileVisitor(Stream.Builder<Path> result) {
            this.result = result;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (IOHelper.hasExtension(file, "jar") || IOHelper.hasExtension(file, "zip"))
                result.accept(file);
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
                Collections.addAll(args, "--userProperties", Launcher.gsonManager.gson.toJson(properties));

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
            Collections.addAll(args, "--versionType", "Launcher v" + Version.getVersion().getVersionString());

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
        else {
            addClientLegacyArgs(args, profile, params);
            System.setProperty("minecraft.applet.TargetDirectory", params.clientDir.toString());
        }
        Collections.addAll(args, profile.getClientArgs());
        List<String> copy = new ArrayList<>(args);
        for (int i = 0, l = copy.size(); i < l; i++) {
            String s = copy.get(i);
            if ( i + 1 < l && ("--accessToken".equals(s) || "--session".equals(s))) {
                copy.set(i + 1, "censored");
            }
        }
        LogHelper.debug("Args: " + copy);
        // Resolve main class and method
        Class<?> mainClass = classLoader.loadClass(profile.getMainClass());
        MethodHandle mainMethod = MethodHandles.publicLookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class)).asFixedArity();
        Launcher.LAUNCHED.set(true);
        JVMHelper.fullGC();
        // Invoke main method
        mainMethod.invokeWithArguments((Object)args.toArray(new String[0]));
    }

	private static Process process = null;
    private static boolean clientStarted = false;
    private static Thread writeParamsThread;
	public static PlayerProfile playerProfile;

    @LauncherAPI
    public static Process launch(
            HashedDir assetHDir, HashedDir clientHDir,
            ClientProfile profile, Params params, boolean pipeOutput) throws Throwable {
        LogHelper.debug("Writing ClientLauncher params");
        ClientLauncherContext context = new ClientLauncherContext();
        clientStarted = false;
        container.write(new ParamContainer(params, profile, assetHDir, clientHDir));
        checkJVMBitsAndVersion();
        LogHelper.debug("Resolving JVM binary");
        Path javaBin = LauncherGuardManager.getGuardJavaBinPath();
        context.javaBin = javaBin;
        context.clientProfile = profile;
        context.playerProfile = params.pp;
        context.args.add(javaBin.toString());
        context.args.add(MAGICAL_INTEL_OPTION);
        if (params.ram > 0 && params.ram <= FunctionalBridge.getJVMTotalMemory()) {
            context.args.add("-Xms" + params.ram + 'M');
            context.args.add("-Xmx" + params.ram + 'M');
        }
        context.args.add(JVMHelper.jvmProperty(LogHelper.DEBUG_PROPERTY, Boolean.toString(LogHelper.isDebugEnabled())));
        context.args.add(JVMHelper.jvmProperty(LogHelper.STACKTRACE_PROPERTY, Boolean.toString(LogHelper.isStacktraceEnabled())));
        context.args.add(JVMHelper.jvmProperty(LogHelper.DEV_PROPERTY, Boolean.toString(LogHelper.isDevEnabled())));
        context.args.add(JVMHelper.jvmProperty(LogHelper.NO_JANSI_PROPERTY, "true")); // Отключаем JAnsi для нормального вывода в DEBUG окно
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
        ClientHookManager.clientLaunchHook.hook(context);
        LauncherGuardManager.guard.addCustomParams(context);
        Collections.addAll(context.args, ClientLauncher.class.getName());
        ClientHookManager.clientLaunchFinallyHook.hook(context);

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
        List<String> command = builder.command();
        // Let's rock!
        ClientHookManager.preStartHook.hook(context, builder);
        process = builder.start();
        if (builder.command() != command) {
        	LogHelper.error("Something strange cheating...");
            System.exit(100);
        	clientStarted = false;
            return null;
        }
        if(ClientHookManager.postStartHook.hook(context, builder)) return process;
        if (!pipeOutput) {
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
                LogHelper.error("Client did not start properly. Enable debug mode for more information");
            }
        }
        clientStarted = false;
        return process;
    }
    public static class ClientLaunchContext
    {
        public final Params params;
        public final ClientProfile profile;
        public final HashedDir assetHDir, clientHDir;
        public DirWatcher assetWatcher, clientWatcher;


        public ClientLaunchContext(Params params, ClientProfile profile, HashedDir assetHDir, HashedDir clientHDir) {
            this.params = params;
            this.profile = profile;
            this.assetHDir = assetHDir;
            this.clientHDir = clientHDir;
        }
    }

    @LauncherAPI
    public static void main(String... args) throws Throwable {
        LauncherEngine engine = LauncherEngine.clientInstance();
        //Launcher.modulesManager = new ClientModuleManager(engine);
        LauncherEngine.modulesManager = new ClientModuleManager();
        LauncherConfig.getAutogenConfig().initModules(); //INIT
        LauncherEngine.modulesManager.initModules(null);
        initGson(LauncherEngine.modulesManager);
        //Launcher.modulesManager.preInitModules();
        if (!LauncherAgent.isStarted()) {
        	NativeJVMHalt.haltA(100);
        	return;
        }
        LauncherEngine.modulesManager.invokeEvent(new PreConfigPhase());
        JVMHelper.verifySystemProperties(ClientLauncher.class, true);
        EnvHelper.checkDangerousParams();
        JVMHelper.checkStackTrace(ClientLauncher.class);
        LogHelper.printVersion("Client Launcher");
        if (engine.runtimeProvider == null) engine.runtimeProvider = new JSRuntimeProvider();
        engine.runtimeProvider.init(true);
        engine.runtimeProvider.preLoad();
        HWIDProvider.registerHWIDs();
        LauncherGuardManager.initGuard(true);
        LogHelper.debug("Reading ClientLauncher params");
        ParamContainer p = container.read();
        Params params = p.params;
        ClientProfile profile = p.profile;
        HashedDir assetHDir = p.assetHDir, clientHDir = p.clientHDir;
        ClientLaunchContext context = new ClientLaunchContext(params, profile, assetHDir, clientHDir);
        Launcher.profile = profile;
        playerProfile = params.pp;
        Request.setSession(params.session);
        checkJVMBitsAndVersion();
        LauncherEngine.modulesManager.invokeEvent(new ClientLauncherInitPhase(context));
        // Verify ClientLauncher sign and classpath
        LogHelper.debug("Verifying ClientLauncher sign and classpath");
        LinkedList<Path> classPath = resolveClassPathList(params.clientDir, profile.getClassPath());
        for (Path classpathURL : classPath) {
            LauncherAgent.addJVMClassPath(classpathURL.normalize().toAbsolutePath());
        }
        profile.pushOptionalClassPath(cp -> {
            LinkedList<Path> optionalClassPath = resolveClassPathList(params.clientDir, cp);
            for (Path classpathURL : optionalClassPath) {
                LauncherAgent.addJVMClassPath(classpathURL.normalize().toAbsolutePath());
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
        Request.service.reconnectCallback = () ->
        {
            LogHelper.debug("WebSocket connect closed. Try reconnect");
            try {
                Request.service.open();
                LogHelper.debug("Connect to %s", Launcher.getConfig().address);
            } catch (Exception e) {
                LogHelper.error(e);
                throw new RequestException(String.format("Connect error: %s", e.getMessage() != null ? e.getMessage() : "null"));
            }
            try {
                RestoreSessionRequest request1 = new RestoreSessionRequest(Request.getSession());
                request1.request();
            } catch (Exception e) {
                LogHelper.error(e);
            }
        };
        AuthService.username = params.pp.username;
        AuthService.uuid = params.pp.uuid;
        ClientService.instrumentation = LauncherAgent.inst;
        ClientService.classLoader = classLoader;
        ClientService.baseURLs = classpathurls;
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
            context.assetWatcher = assetWatcher;
            context.clientWatcher = clientWatcher;
            Launcher.profile.pushOptionalFile(clientHDir, false);
            LauncherEngine.modulesManager.invokeEvent(new ClientLauncherPostInitPhase(context));
            // Start WatchService, and only then client
            CommonHelper.newThread("Asset Directory Watcher", true, assetWatcher).start();
            CommonHelper.newThread("Client Directory Watcher", true, clientWatcher).start();
            verifyHDir(params.assetDir, assetHDir, assetMatcher, digest);
            verifyHDir(params.clientDir, clientHDir, clientMatcher, digest);
            LauncherEngine.modulesManager.invokeEvent(new ClientLaunchPhase(context));
            launch(profile, params);
        }
    }

    private static URL[] resolveClassPath(Path clientDir, String... classPath) throws IOException {
        return resolveClassPathStream(clientDir, classPath).map(IOHelper::toURL).toArray(URL[]::new);
    }

    private static LinkedList<Path> resolveClassPathList(Path clientDir, String... classPath) throws IOException {
        return resolveClassPathStream(clientDir, classPath).collect(Collectors.toCollection(LinkedList::new));
    }

    private static Stream<Path> resolveClassPathStream(Path clientDir, String... classPath) throws IOException {
        Stream.Builder<Path> builder = Stream.builder();
        for (String classPathEntry : classPath) {
            Path path = clientDir.resolve(IOHelper.toPath(classPathEntry));
            if (IOHelper.isDir(path)) { // Recursive walking and adding
                IOHelper.walk(path, new ClassPathFileVisitor(builder), false);
                continue;
            }
            builder.accept(path);
        }
        return builder.build();
    }

    private static void initGson(ClientModuleManager moduleManager) {
        Launcher.gsonManager = new ClientGsonManager(moduleManager);
        Launcher.gsonManager.initGson();
    }

    @LauncherAPI
    public static void setProfile(ClientProfile profile) {
        Launcher.profile = profile;
        LogHelper.debug("New Profile name: %s", profile.getTitle());
    }

    public static void verifyHDir(Path dir, HashedDir hdir, FileNameMatcher matcher, boolean digest) throws IOException {
        //if (matcher != null)
        //    matcher = matcher.verifyOnly();

        // Hash directory and compare (ignore update-only matcher entries, it will break offline-mode)
        HashedDir currentHDir = new HashedDir(dir, matcher, true, digest);
        HashedDir.Diff diff = hdir.diff(currentHDir, matcher);
        if (!diff.isSame()) {
            /*AtomicBoolean isFoundFile = new AtomicBoolean(false);
            diff.extra.walk(File.separator, (e,k,v) -> {
                if(v.getType().equals(HashedEntry.Type.FILE)) { LogHelper.error("Extra file %s", e); isFoundFile.set(true); }
                else LogHelper.error("Extra %s", e);
            });
            diff.mismatch.walk(File.separator, (e,k,v) -> {
                if(v.getType().equals(HashedEntry.Type.FILE)) { LogHelper.error("Mismatch file %s", e); isFoundFile.set(true); }
                else LogHelper.error("Mismatch %s", e);
            });
            if(isFoundFile.get())*/
            throw new SecurityException(String.format("Forbidden modification: '%s'", IOHelper.getFileName(dir)));
        }
    }

    private ClientLauncher() {
    }

    public static interface ParamsAPI {
    	ParamContainer read() throws Exception;
    	void write(ParamContainer p) throws Exception;
    }
    
    public static ParamsAPI container = new ParamsAPI() {
		@Override
		public ParamContainer read() throws Exception {
			ParamContainer p = new ParamContainer();
            try (Socket socket = IOHelper.newSocket()) {
                socket.connect(new InetSocketAddress(SOCKET_HOST, SOCKET_PORT));
                try (HInput input = new HInput(socket.getInputStream())) {
                    p.params = new Params(input);
                    p.profile = Launcher.gsonManager.gson.fromJson(input.readString(0), ClientProfile.class);
                    p.assetHDir = new HashedDir(input);
                    p.clientHDir = new HashedDir(input);
                    ClientHookManager.paramsInputHook.hook(input);
                }
            }
            return p;
		}
		@Override
		public void write(ParamContainer p) throws Exception {
	        if (writeParamsThread != null && writeParamsThread.isAlive())
	            writeParamsThread.interrupt();
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
	                        p.params.write(output);
	                        output.writeString(Launcher.gsonManager.gson.toJson(p.profile), 0);
	                        p.assetHDir.write(output);
	                        p.clientHDir.write(output);
	                        ClientHookManager.paramsOutputHook.hook(output);
	                    }
	                    clientStarted = true;
	                }
	            } catch (IOException e) {
	                LogHelper.error(e);
	            }
	        });
	        writeParamsThread.start();
		}
    	
    };

    public static class ParamContainer extends StreamObject {

        public ParamContainer(HInput input) throws Exception {
            params = new Params(input);
            profile = Launcher.gsonManager.gson.fromJson(input.readString(0), ClientProfile.class);
            assetHDir = new HashedDir(input);
            clientHDir = new HashedDir(input);
            ClientHookManager.paramsInputHook.hook(input);
        }

        public ParamContainer() {
        }

        public ParamContainer(Params params, ClientProfile profile,  HashedDir assetHDir, HashedDir clientHDir) {
        	this.params = params;
        	this.profile = profile;
        	this.assetHDir = assetHDir;
        	this.clientHDir = clientHDir;
        }

        public Params params;
        public ClientProfile profile;
        public HashedDir assetHDir, clientHDir;

		@Override
		public void write(HOutput output) throws IOException {
            params.write(output);
            output.writeString(Launcher.gsonManager.gson.toJson(profile), 0);
            assetHDir.write(output);
            clientHDir.write(output);
            ClientHookManager.paramsOutputHook.hook(output);
		}
    }
}
