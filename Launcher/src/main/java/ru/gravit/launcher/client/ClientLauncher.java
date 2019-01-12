package ru.gravit.launcher.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.gravit.launcher.*;
import ru.gravit.launcher.gui.JSRuntimeProvider;
import ru.gravit.launcher.hasher.DirWatcher;
import ru.gravit.launcher.hasher.FileNameMatcher;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.update.LegacyLauncherRequest;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
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
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.security.interfaces.RSAPublicKey;
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
        public final Set<ClientProfile.OptionalFile> updateOptional;
        @LauncherAPI
        public final Set<ClientProfile.OptionalArgs> optionalClientArgs;
        @LauncherAPI
        public final Set<ClientProfile.OptionalArgs> optionalClassPath;
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
        private final byte[] launcherDigest;
        @LauncherAPI
        public final long session;

        @LauncherAPI
        public Params(byte[] launcherDigest, Path assetDir, Path clientDir, PlayerProfile pp, String accessToken,
                      boolean autoEnter, boolean fullScreen, int ram, int width, int height) {
            this.launcherDigest = launcherDigest.clone();
            this.updateOptional = new HashSet<>();
            this.optionalClientArgs = new HashSet<>();
            this.optionalClassPath = new HashSet<>();
            for (ClientProfile.OptionalFile s : Launcher.profile.getOptional()) {
                if (s.mark) updateOptional.add(s);
            }
            for(ClientProfile.OptionalArgs s : Launcher.profile.getOptionalClientArgs())
            {
                if(s.mark) optionalClientArgs.add(s);
            }
            for(ClientProfile.OptionalArgs s : Launcher.profile.getOptionalClassPath())
            {
                if(s.mark) optionalClassPath.add(s);
            }
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
            launcherDigest = input.readByteArray(0);
            session = input.readLong();
            // Client paths
            assetDir = IOHelper.toPath(input.readString(0));
            clientDir = IOHelper.toPath(input.readString(0));
            updateOptional = new HashSet<>();
            optionalClientArgs = new HashSet<>();
            optionalClassPath = new HashSet<>();
            int len = input.readLength(128);
            for (int i = 0; i < len; ++i) {
                String file = input.readString(512);
                boolean mark = input.readBoolean();
                updateOptional.add(new ClientProfile.OptionalFile(file, mark));
            }
            len = input.readLength(256);
            for (int i = 0; i < len; ++i) {
                int len2 = input.readLength(16);
                boolean mark = input.readBoolean();
                String[] optArgs = new String[len];
                for(int j=0;j<len2;++j) optArgs[j] = input.readString(512);
                optionalClientArgs.add(new ClientProfile.OptionalArgs(optArgs, mark));
            }
            len = input.readLength(256);
            for (int i = 0; i < len; ++i) {
                int len2 = input.readLength(16);
                boolean mark = input.readBoolean();
                String[] optArgs = new String[len];
                for(int j=0;j<len2;++j) optArgs[j] = input.readString(512);
                optionalClassPath.add(new ClientProfile.OptionalArgs(optArgs, mark));
            }
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
            output.writeByteArray(launcherDigest, 0);
            output.writeLong(session);
            // Client paths
            output.writeString(assetDir.toString(), 0);
            output.writeString(clientDir.toString(), 0);
            output.writeLength(updateOptional.size(), 128);
            for (ClientProfile.OptionalFile s : updateOptional) {
                output.writeString(s.file, 512);
                output.writeBoolean(s.mark);
            }
            output.writeLength(optionalClientArgs.size(),256);
            for(ClientProfile.OptionalArgs s : optionalClientArgs)
            {
                output.writeLength(s.args.length,16);
                output.writeBoolean(s.mark);
                for(String f : s.args) output.writeString(f,512);
            }
            output.writeLength(optionalClassPath.size(),256);
            for(ClientProfile.OptionalArgs s : optionalClassPath)
            {
                output.writeLength(s.args.length,16);
                output.writeBoolean(s.mark);
                for(String f : s.args) output.writeString(f,512);
            }
            // Client params
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
    private static final boolean isUsingWrapper = Launcher.getConfig().isUsingWrapper;
    private static final boolean isDownloadJava = Launcher.getConfig().isDownloadJava;

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
    public static class ClientUserProperties
    {
        @LauncherAPI
        String[] skinURL;
        @LauncherAPI
        String[] skinDigest;
        @LauncherAPI
        String[] cloakURL;
        @LauncherAPI
        String[] cloakDigest;
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
        for(ClientProfile.OptionalArgs optionalArgs : params.optionalClientArgs)
        {
            if(optionalArgs.mark) Collections.addAll(args,optionalArgs.args);
        }
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


    public static boolean isUsingWrapper() {
        return JVMHelper.OS_TYPE == OS.MUSTDIE && isUsingWrapper;
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

    @LauncherAPI
    public static Process launch(
            SignedObjectHolder<HashedDir> assetHDir, SignedObjectHolder<HashedDir> clientHDir,
            ClientProfile profile, Params params, boolean pipeOutput) throws Throwable {
        // Write params file (instead of CLI; Mustdie32 API can't handle command line > 32767 chars)
        LogHelper.debug("Writing ClientLauncher params");
        CommonHelper.newThread("Client params writter", false, () ->
        {
            try {
                try (ServerSocket socket = new ServerSocket()) {

                    socket.setReuseAddress(true);
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
                        output.writeString(Launcher.gson.toJson(profile),0);
                        assetHDir.write(output);
                        clientHDir.write(output);
                    }


                }
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }).start();
        checkJVMBitsAndVersion();
        // Fill CLI arguments
        List<String> args = new LinkedList<>();
        boolean wrapper = isUsingWrapper();
        LogHelper.debug("Resolving JVM binary");
        Path javaBin = null;
        if (isDownloadJava) {
            //Linux и Mac не должны скачивать свою JVM
            if (JVMHelper.OS_TYPE == OS.MUSTDIE)
                javaBin = IOHelper.resolveJavaBin(JavaBinPath);
            else
                javaBin = IOHelper.resolveJavaBin(Paths.get(System.getProperty("java.home")));
        } else
            javaBin = IOHelper.resolveJavaBin(Paths.get(System.getProperty("java.home")));
        args.add(javaBin.toString());
        args.add(MAGICAL_INTEL_OPTION);
        if (params.ram > 0 && params.ram <= JVMHelper.RAM) {
            args.add("-Xms" + params.ram + 'M');
            args.add("-Xmx" + params.ram + 'M');
        }
        args.add(JVMHelper.jvmProperty(LogHelper.DEBUG_PROPERTY, Boolean.toString(LogHelper.isDebugEnabled())));
        args.add(JVMHelper.jvmProperty(LogHelper.STACKTRACE_PROPERTY, Boolean.toString(LogHelper.isStacktraceEnabled())));
        if (LauncherConfig.ADDRESS_OVERRIDE != null)
            args.add(JVMHelper.jvmProperty(LauncherConfig.ADDRESS_OVERRIDE_PROPERTY, LauncherConfig.ADDRESS_OVERRIDE));
        if (JVMHelper.OS_TYPE == OS.MUSTDIE) {
            if (JVMHelper.OS_VERSION.startsWith("10.")) {
                LogHelper.debug("MustDie 10 fix is applied");
                args.add(JVMHelper.jvmProperty("os.name", "Windows 10"));
                args.add(JVMHelper.jvmProperty("os.version", "10.0"));
            }
            args.add(JVMHelper.systemToJvmProperty("avn32"));
            args.add(JVMHelper.systemToJvmProperty("avn64"));
        }
        // Add classpath and main class
        String pathLauncher = IOHelper.getCodeSource(ClientLauncher.class).toString();
        Collections.addAll(args, profile.getJvmArgs());
        if(profile.getOptionalJVMArgs() != null)
        {
            for(ClientProfile.OptionalArgs addArgs : profile.getOptionalJVMArgs())
            {
                if(addArgs.mark) Collections.addAll(args,addArgs.args);
            }
        }
        Collections.addAll(args, "-Djava.library.path=".concat(params.clientDir.resolve(NATIVES_DIR).toString())); // Add Native Path
        Collections.addAll(args, "-javaagent:".concat(pathLauncher));
        if (wrapper)
            Collections.addAll(args, "-Djava.class.path=".concat(pathLauncher)); // Add Class Path
        else {
            Collections.addAll(args, "-cp");
            Collections.addAll(args, pathLauncher);
        }
        Collections.addAll(args, ClientLauncher.class.getName());

        // Print commandline debug message
        LogHelper.debug("Commandline: " + args);

        // Build client process
        LogHelper.debug("Launching client instance");
        ProcessBuilder builder = new ProcessBuilder(args);
        if (wrapper)
            builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
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
        return process;
    }

    @LauncherAPI
    public static void main(String... args) throws Throwable {
        LauncherEngine engine = LauncherEngine.clientInstance();
        Launcher.modulesManager = new ClientModuleManager(engine);
        LauncherConfig.getAutogenConfig().initModules(); //INIT
        initGson();
        Launcher.modulesManager.preInitModules();
        checkJVMBitsAndVersion();
        JVMHelper.verifySystemProperties(ClientLauncher.class, true);
        EnvHelper.checkDangerousParams();
        JVMHelper.checkStackTrace(ClientLauncher.class);
        LogHelper.printVersion("Client Launcher");
        if(engine.runtimeProvider == null) engine.runtimeProvider = new JSRuntimeProvider();
        engine.runtimeProvider.init(true);
        engine.runtimeProvider.preLoad();
        // Read and delete params file
        LogHelper.debug("Reading ClientLauncher params");
        Params params;
        ClientProfile profile;
        SignedObjectHolder<HashedDir> assetHDir, clientHDir;
        RSAPublicKey publicKey = Launcher.getConfig().publicKey;
        try {
            try (Socket socket = IOHelper.newSocket()) {
                socket.connect(new InetSocketAddress(SOCKET_HOST, SOCKET_PORT));
                try (HInput input = new HInput(socket.getInputStream())) {
                    params = new Params(input);
                    profile = gson.fromJson(input.readString(0),ClientProfile.class);

                    // Read hdirs
                    assetHDir = new SignedObjectHolder<>(input, publicKey, HashedDir::new);
                    clientHDir = new SignedObjectHolder<>(input, publicKey, HashedDir::new);
                }
            }
        } catch (IOException ex) {
            LogHelper.error(ex);
            System.exit(-98);
            return;
        }
        Launcher.profile = profile;
        Request.setSession(params.session);
        Launcher.modulesManager.initModules();
        // Verify ClientLauncher sign and classpath
        LogHelper.debug("Verifying ClientLauncher sign and classpath");
        //TODO: GO TO DIGEST
        //SecurityHelper.verifySign(LegacyLauncherRequest.BINARY_PATH, params.launcherDigest, publicKey);
        LinkedList<Path> classPath = resolveClassPathList(params.clientDir, profile.getClassPath());
        for (Path classpathURL : classPath) {
            LauncherAgent.addJVMClassPath(classpathURL.toAbsolutePath().toString());
        }
        for(ClientProfile.OptionalArgs optionalArgs : params.optionalClassPath)
        {
            if(!optionalArgs.mark) continue;
            LinkedList<Path> optionalClassPath = resolveClassPathList(params.clientDir, optionalArgs.args);
            for (Path classpathURL : optionalClassPath) {
                LauncherAgent.addJVMClassPath(classpathURL.toAbsolutePath().toString());
            }
        }

        URL[] classpathurls = resolveClassPath(params.clientDir, profile.getClassPath());
        classLoader = new PublicURLClassLoader(classpathurls, ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        classLoader.nativePath = params.clientDir.resolve(NATIVES_DIR).toString();
        PublicURLClassLoader.systemclassloader = classLoader;
        // Start client with WatchService monitoring
        boolean digest = !profile.isUpdateFastCheck();
        LogHelper.debug("Starting JVM and client WatchService");
        FileNameMatcher assetMatcher = profile.getAssetUpdateMatcher();
        FileNameMatcher clientMatcher = profile.getClientUpdateMatcher();
        try (DirWatcher assetWatcher = new DirWatcher(params.assetDir, assetHDir.object, assetMatcher, digest);
             DirWatcher clientWatcher = new DirWatcher(params.clientDir, clientHDir.object, clientMatcher, digest)) {
            // Verify current state of all dirs
            //verifyHDir(IOHelper.JVM_DIR, jvmHDir.object, null, digest);
            HashedDir hdir = clientHDir.object;
            for (ClientProfile.OptionalFile s : Launcher.profile.getOptional()) {
                if (params.updateOptional.contains(s)) s.mark = true;
                else hdir.removeR(s.file);
            }
            verifyHDir(params.assetDir, assetHDir.object, assetMatcher, digest);
            verifyHDir(params.clientDir, hdir, clientMatcher, digest);
            Launcher.modulesManager.postInitModules();
            // Start WatchService, and only then client
            CommonHelper.newThread("Asset Directory Watcher", true, assetWatcher).start();
            CommonHelper.newThread("Client Directory Watcher", true, clientWatcher).start();
            launch(profile, params);
        }
    }

    @LauncherAPI
    public void launchLocal(SignedObjectHolder<HashedDir> assetHDir, SignedObjectHolder<HashedDir> clientHDir,
                            ClientProfile profile, Params params) throws Throwable {
        RSAPublicKey publicKey = Launcher.getConfig().publicKey;
        LogHelper.debug("Verifying ClientLauncher sign and classpath");
        SecurityHelper.verifySign(LegacyLauncherRequest.BINARY_PATH, params.launcherDigest, publicKey);
        LinkedList<Path> classPath = resolveClassPathList(params.clientDir, profile.getClassPath());
        for (Path classpathURL : classPath) {
            LauncherAgent.addJVMClassPath(classpathURL.toAbsolutePath().toString());
        }
        URL[] classpathurls = resolveClassPath(params.clientDir, profile.getClassPath());
        classLoader = new PublicURLClassLoader(classpathurls, ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        classLoader.nativePath = params.clientDir.resolve(NATIVES_DIR).toString();
        PublicURLClassLoader.systemclassloader = classLoader;
        // Start client with WatchService monitoring
        boolean digest = !profile.isUpdateFastCheck();
        LogHelper.debug("Starting JVM and client WatchService");
        FileNameMatcher assetMatcher = profile.getAssetUpdateMatcher();
        FileNameMatcher clientMatcher = profile.getClientUpdateMatcher();
        try (DirWatcher assetWatcher = new DirWatcher(params.assetDir, assetHDir.object, assetMatcher, digest);
             DirWatcher clientWatcher = new DirWatcher(params.clientDir, clientHDir.object, clientMatcher, digest)) {
            // Verify current state of all dirs
            //verifyHDir(IOHelper.JVM_DIR, jvmHDir.object, null, digest);
            HashedDir hdir = clientHDir.object;
            for (ClientProfile.OptionalFile s : Launcher.profile.getOptional()) {
                if (params.updateOptional.contains(s)) s.mark = true;
                else hdir.removeR(s.file);
            }
            verifyHDir(params.assetDir, assetHDir.object, assetMatcher, digest);
            verifyHDir(params.clientDir, hdir, clientMatcher, digest);
            Launcher.modulesManager.postInitModules();
            // Start WatchService, and only then client
            CommonHelper.newThread("Asset Directory Watcher", true, assetWatcher).start();
            CommonHelper.newThread("Client Directory Watcher", true, clientWatcher).start();
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

    public static void initGson()
    {
        if(Launcher.gson != null) return;
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
