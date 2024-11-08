package pro.gravit.launcher.client;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.base.api.AuthService;
import pro.gravit.launcher.base.api.ClientService;
import pro.gravit.launcher.base.api.KeyService;
import pro.gravit.launcher.client.events.*;
import pro.gravit.launcher.core.hasher.FileNameMatcher;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launcher.core.hasher.HashedEntry;
import pro.gravit.launcher.base.modules.events.PreConfigPhase;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileVersions;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalActionClassPath;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalActionClientArgs;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.RequestException;
import pro.gravit.launcher.base.request.RequestService;
import pro.gravit.launcher.base.request.websockets.StdWebSocketService;
import pro.gravit.launcher.core.serialize.HInput;
import pro.gravit.launcher.client.utils.DirWatcher;
import pro.gravit.utils.helper.*;
import pro.gravit.utils.launch.*;

import javax.crypto.CipherInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientLauncherEntryPoint {
    public static ClientModuleManager modulesManager;
    public static ClientParams clientParams;

    private static Launch launch;
    private static ClassLoaderControl classLoaderControl;

    private static ClientParams readParams(SocketAddress address) throws IOException {
        try (Socket socket = IOHelper.newSocket()) {
            socket.connect(address);
            try (HInput input = new HInput(new CipherInputStream(socket.getInputStream(), SecurityHelper.newAESDecryptCipher(SecurityHelper.fromHex(Launcher.getConfig().secretKeyClient))))) {
                byte[] serialized = input.readByteArray(0);
                ClientParams params = Launcher.gsonManager.gson.fromJson(IOHelper.decode(serialized), ClientParams.class);
                params.clientHDir = new HashedDir(input);
                params.assetHDir = new HashedDir(input);
                boolean isNeedReadJavaDir = input.readBoolean();
                if (isNeedReadJavaDir)
                    params.javaHDir = new HashedDir(input);
                return params;
            }
        }
    }

    public static void main(String[] args) {
        JVMHelper.verifySystemProperties(ClientLauncherEntryPoint.class, true);
        EnvHelper.checkDangerousParams();
        JVMHelper.checkStackTrace(ClientLauncherEntryPoint.class);
        LogHelper.printVersion("Client Launcher");
        ClientLauncherMethods.checkClass(ClientLauncherEntryPoint.class);
        try {
            realMain(args);
        } catch (Throwable e) {
            LogHelper.error(e);
        }
    }

    private static void realMain(String[] args) throws Throwable {
        modulesManager = new ClientModuleManager();
        modulesManager.loadModule(new ClientLauncherCoreModule());
        LauncherConfig.initModules(modulesManager); //INIT
        modulesManager.initModules(null);
        ClientLauncherMethods.initGson(modulesManager);
        modulesManager.invokeEvent(new PreConfigPhase());
        LogHelper.debug("Reading ClientLauncher params");
        ClientParams params = readParams(new InetSocketAddress("127.0.0.1", Launcher.getConfig().clientPort));
        ClientLauncherMethods.verifyNoAgent();
        if(params.timestamp > System.currentTimeMillis() || params.timestamp + 30*1000 < System.currentTimeMillis() ) {
            LogHelper.error("Timestamp failed: current %d | params %d | diff %d", System.currentTimeMillis(), params.timestamp, System.currentTimeMillis() - params.timestamp);
            ClientLauncherMethods.exitLauncher(-662);
            return;
        }
        ClientProfile profile = params.profile;
        Launcher.profile = profile;
        AuthService.profile = profile;
        clientParams = params;
        if (params.oauth != null) {
            LogHelper.info("Using OAuth");
            if (params.oauthExpiredTime != 0) {
                Request.setOAuth(params.authId, params.oauth, params.oauthExpiredTime);
            } else {
                Request.setOAuth(params.authId, params.oauth);
            }
            if (params.extendedTokens != null) {
                Request.addAllExtendedToken(params.extendedTokens);
            }
        } else if (params.session != null) {
            throw new UnsupportedOperationException("Legacy session not supported");
        }
        modulesManager.invokeEvent(new ClientProcessInitPhase(params));

        Path clientDir = Paths.get(params.clientDir);
        Path assetDir = Paths.get(params.assetDir);

        // Verify ClientLauncher sign and classpath
        LogHelper.debug("Verifying ClientLauncher sign and classpath");
        Set<Path> ignoredPath = new HashSet<>();
        List<Path> classpath = resolveClassPath(ignoredPath, clientDir, params.actions, params.profile)
                .collect(Collectors.toCollection(ArrayList::new));
        if(LogHelper.isDevEnabled()) {
            for(var e : classpath) {
                LogHelper.dev("Classpath entry %s", e);
            }
        }
        List<URL> classpathURLs = classpath.stream().map(IOHelper::toURL).collect(Collectors.toList());
        // Start client with WatchService monitoring
        RequestService service;
        if (params.offlineMode) {
            service = ClientLauncherMethods.initOffline(modulesManager, params);
            Request.setRequestService(service);
        } else {
            service = StdWebSocketService.initWebSockets(Launcher.getConfig().address).get();
            Request.setRequestService(service);
            LogHelper.debug("Restore sessions");
            Request.restore(false, false, true);
            service.registerEventHandler(new BasicLauncherEventHandler());
            ((StdWebSocketService) service).reconnectCallback = () ->
            {
                LogHelper.debug("WebSocket connect closed. Try reconnect");
                try {
                    Request.reconnect();
                } catch (Exception e) {
                    LogHelper.error(e);
                    throw new RequestException("Connection failed", e);
                }
            };
        }
        LogHelper.debug("Natives dir %s", params.nativesDir);
        ClientProfile.ClassLoaderConfig classLoaderConfig = profile.getClassLoaderConfig();
        LaunchOptions options = new LaunchOptions();
        options.enableHacks = profile.hasFlag(ClientProfile.CompatibilityFlags.ENABLE_HACKS);
        options.moduleConf = profile.getModuleConf();
        ClientService.nativePath = params.nativesDir;
        if(profile.getLoadNatives() != null) {
            for(String e : profile.getLoadNatives()) {
                System.load(Paths.get(params.nativesDir).resolve(ClientService.findLibrary(e)).toAbsolutePath().toString());
            }
        }
        if (classLoaderConfig == ClientProfile.ClassLoaderConfig.LAUNCHER || classLoaderConfig == ClientProfile.ClassLoaderConfig.MODULE) {
            if(JVMHelper.JVM_VERSION <= 11) {
                launch = new LegacyLaunch();
            } else {
                launch = new ModuleLaunch();
            }
            classLoaderControl = launch.init(classpath, params.nativesDir, options);
            System.setProperty("java.class.path", classpath.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator)));
            modulesManager.invokeEvent(new ClientProcessClassLoaderEvent(launch, classLoaderControl, profile));
            ClientService.baseURLs = classLoaderControl.getURLs();
        } else if (classLoaderConfig == ClientProfile.ClassLoaderConfig.SYSTEM_ARGS) {
            launch = new BasicLaunch();
            classLoaderControl = launch.init(classpath, params.nativesDir, options);
            ClientService.baseURLs = classpathURLs.toArray(new URL[0]);
        } else {
            throw new UnsupportedOperationException(String.format("Unknown classLoaderConfig %s", classLoaderConfig));
        }
        if(profile.hasFlag(ClientProfile.CompatibilityFlags.CLASS_CONTROL_API)) {
            ClientService.classLoaderControl = classLoaderControl;
        }
        if(params.lwjglGlfwWayland && profile.hasFlag(ClientProfile.CompatibilityFlags.WAYLAND_USE_CUSTOM_GLFW)) {
            String glfwName = ClientService.findLibrary("glfw_wayland");
            System.setProperty("org.lwjgl.glfw.libname", glfwName);
        }
        AuthService.projectName = Launcher.getConfig().projectName;
        AuthService.username = params.playerProfile.username;
        AuthService.uuid = params.playerProfile.uuid;
        KeyService.serverRsaPublicKey = Launcher.getConfig().rsaPublicKey;
        KeyService.serverEcPublicKey = Launcher.getConfig().ecdsaPublicKey;
        modulesManager.invokeEvent(new ClientProcessReadyEvent(params));
        LogHelper.debug("Starting JVM and client WatchService");
        FileNameMatcher assetMatcher = profile.getAssetUpdateMatcher();
        FileNameMatcher clientMatcher = profile.getClientUpdateMatcher();
        Path javaDir = Paths.get(System.getProperty("java.home"));
        try (DirWatcher assetWatcher = new DirWatcher(assetDir, params.assetHDir, assetMatcher, true);
             DirWatcher clientWatcher = new DirWatcher(clientDir, params.clientHDir, clientMatcher, true);
             DirWatcher javaWatcher = params.javaHDir == null ? null : new DirWatcher(javaDir, params.javaHDir, null, true)) {
            // Verify current state of all dirs
            //verifyHDir(IOHelper.JVM_DIR, jvmHDir.object, null, digest);
            //for (OptionalFile s : Launcher.profile.getOptional()) {
            //    if (params.updateOptional.contains(s)) s.mark = true;
            //    else hdir.removeR(s.file);
            //}
            // Start WatchService, and only then client
            CommonHelper.newThread("Asset Directory Watcher", true, assetWatcher).start();
            CommonHelper.newThread("Client Directory Watcher", true, clientWatcher).start();
            if (javaWatcher != null)
                CommonHelper.newThread("Java Directory Watcher", true, javaWatcher).start();
            verifyHDir(assetDir, params.assetHDir, assetMatcher, false, false);
            verifyHDir(clientDir, params.clientHDir, clientMatcher, false, true);
            if (javaWatcher != null)
                verifyHDir(javaDir, params.javaHDir, null, false, true);
            modulesManager.invokeEvent(new ClientProcessLaunchEvent(params));
            launch(profile, params);
        }
    }

    public static void verifyHDir(Path dir, HashedDir hdir, FileNameMatcher matcher, boolean digest, boolean checkExtra) throws IOException {
        //if (matcher != null)
        //    matcher = matcher.verifyOnly();

        // Hash directory and compare (ignore update-only matcher entries, it will break offline-mode)
        HashedDir currentHDir = new HashedDir(dir, matcher, true, digest);
        HashedDir.Diff diff = hdir.diff(currentHDir, matcher);
        AtomicReference<String> latestPath = new AtomicReference<>("unknown");
        if (!diff.mismatch.isEmpty() || (checkExtra && !diff.extra.isEmpty())) {
            diff.extra.walk(File.separator, (e, k, v) -> {
                if (v.getType().equals(HashedEntry.Type.FILE)) {
                    LogHelper.error("Extra file %s", e);
                    latestPath.set(e);
                } else LogHelper.error("Extra %s", e);
                return HashedDir.WalkAction.CONTINUE;
            });
            diff.mismatch.walk(File.separator, (e, k, v) -> {
                if (v.getType().equals(HashedEntry.Type.FILE)) {
                    LogHelper.error("Mismatch file %s", e);
                    latestPath.set(e);
                } else LogHelper.error("Mismatch %s", e);
                return HashedDir.WalkAction.CONTINUE;
            });
            throw new SecurityException(String.format("Forbidden modification: '%s' file '%s'", IOHelper.getFileName(dir), latestPath.get()));
        }
    }

    private static LinkedList<Path> resolveClassPathList(Set<Path> ignorePaths, Path clientDir, List<String> classPath) throws IOException {
        return resolveClassPathStream(ignorePaths, clientDir, classPath).collect(Collectors.toCollection(LinkedList::new));
    }

    private static Stream<Path> resolveClassPathStream(Set<Path> ignorePaths, Path clientDir, List<String> classPath) throws IOException {
        Stream.Builder<Path> builder = Stream.builder();
        for (String classPathEntry : classPath) {
            Path path = clientDir.resolve(IOHelper.toPath(classPathEntry.replace(IOHelper.CROSS_SEPARATOR, IOHelper.PLATFORM_SEPARATOR)));
            if (IOHelper.isDir(path)) { // Recursive walking and adding
                List<Path> jars = new ArrayList<>(32);
                IOHelper.walk(path, new ClassPathFileVisitor(jars), false);
                Collections.sort(jars);
                for(var e : jars) {
                    if(ignorePaths.contains(e)) {
                        continue;
                    }
                    builder.accept(e);
                    ignorePaths.add(e);
                }
                continue;
            }
            if(ignorePaths.contains(path)) {
                continue;
            }
            builder.accept(path);
            ignorePaths.add(path);
        }
        return builder.build();
    }

    public static Stream<Path> resolveClassPath(Set<Path> ignorePaths, Path clientDir, Set<OptionalAction> actions, ClientProfile profile) throws IOException {
        Stream<Path> result = resolveClassPathStream(ignorePaths, clientDir, profile.getClassPath());
        for (OptionalAction a : actions) {
            if (a instanceof OptionalActionClassPath)
                result = Stream.concat(result, resolveClassPathStream(ignorePaths, clientDir, ((OptionalActionClassPath) a).args));
        }
        return result;
    }

    private static void launch(ClientProfile profile, ClientParams params) throws Throwable {
        // Add client args
        Collection<String> args = new LinkedList<>();
        if (profile.getVersion().compareTo(ClientProfileVersions.MINECRAFT_1_6_4) >= 0)
            params.addClientArgs(args);
        else {
            params.addClientLegacyArgs(args);
            System.setProperty("minecraft.applet.TargetDirectory", params.clientDir);
        }
        args.addAll(profile.getClientArgs());
        for (OptionalAction action : params.actions) {
            if (action instanceof OptionalActionClientArgs) {
                args.addAll(((OptionalActionClientArgs) action).args);
            }
        }
        List<String> copy = new ArrayList<>(args);
        for (int i = 0, l = copy.size(); i < l; i++) {
            String s = copy.get(i);
            if (i + 1 < l && ("--accessToken".equals(s) || "--session".equals(s))) {
                copy.set(i + 1, "censored");
            }
        }
        LogHelper.debug("Args: " + copy);
        // Resolve main class and method
        modulesManager.invokeEvent(new ClientProcessPreInvokeMainClassEvent(params, profile, args));
        // Invoke main method
        try {
            {
                List<String> compatClasses = profile.getCompatClasses();
                for (String e : compatClasses) {
                    Class<?> clazz = classLoaderControl.getClass(e);
                    MethodHandle runMethod = MethodHandles.lookup().findStatic(clazz, "run", MethodType.methodType(void.class, ClassLoaderControl.class));
                    runMethod.invoke(classLoaderControl);
                }
            }
            Launcher.LAUNCHED.set(true);
            JVMHelper.fullGC();
            launch.launch(params.profile.getMainClass(), params.profile.getMainModule(), args);
            LogHelper.debug("Main exit successful");
        } catch (Throwable e) {
            LogHelper.error(e);
            throw e;
        } finally {
            ClientLauncherMethods.exitLauncher(0);
        }

    }

    private static final class ClassPathFileVisitor extends SimpleFileVisitor<Path> {
        private final List<Path> result;

        private ClassPathFileVisitor(List<Path> result) {
            this.result = result;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (IOHelper.hasExtension(file, "jar") || IOHelper.hasExtension(file, "zip")) {
                result.add(file);
            }
            return super.visitFile(file, attrs);
        }
    }
}
