package pro.gravit.launcher.client;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherAgent;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.api.AuthService;
import pro.gravit.launcher.api.ClientService;
import pro.gravit.launcher.client.events.client.*;
import pro.gravit.launcher.guard.LauncherGuardManager;
import pro.gravit.launcher.hasher.FileNameMatcher;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.managers.ClientGsonManager;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launcher.patches.FMLPatcher;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.auth.RestoreSessionRequest;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.utils.DirWatcher;
import pro.gravit.utils.helper.*;

import javax.swing.*;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientLauncherEntryPoint {
    private static ClientClassLoader classLoader;

    private static ClientLauncherProcess.ClientParams readParams(SocketAddress address) throws IOException {
        try (Socket socket = IOHelper.newSocket()) {
            socket.connect(address);
            try (HInput input = new HInput(socket.getInputStream())) {
                byte[] serialized = input.readByteArray(0);
                ClientLauncherProcess.ClientParams params = Launcher.gsonManager.gson.fromJson(new String(serialized, IOHelper.UNICODE_CHARSET), ClientLauncherProcess.ClientParams.class);
                params.clientHDir = new HashedDir(input);
                params.assetHDir = new HashedDir(input);
                params.javaHDir = new HashedDir(input);
                return params;
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        LauncherEngine.IS_CLIENT.set(true);
        LauncherEngine engine = LauncherEngine.clientInstance();
        JVMHelper.verifySystemProperties(ClientLauncherEntryPoint.class, true);
        EnvHelper.checkDangerousParams();
        JVMHelper.checkStackTrace(ClientLauncherEntryPoint.class);
        LogHelper.printVersion("Client Launcher");
        LauncherEngine.checkClass(LauncherEngine.class);
        LauncherEngine.checkClass(LauncherAgent.class);
        LauncherEngine.checkClass(ClientLauncherEntryPoint.class);
        LauncherEngine.modulesManager = new ClientModuleManager();
        LauncherEngine.modulesManager.loadModule(new ClientLauncherCoreModule());
        LauncherConfig.initModules(LauncherEngine.modulesManager); //INIT
        LauncherEngine.modulesManager.initModules(null);
        initGson(LauncherEngine.modulesManager);
        LauncherEngine.verifyNoAgent();
        LauncherEngine.modulesManager.invokeEvent(new PreConfigPhase());
        engine.readKeys();
        LauncherGuardManager.initGuard(true);
        LogHelper.debug("Reading ClientLauncher params");
        ClientLauncherProcess.ClientParams params = readParams(new InetSocketAddress("127.0.0.1", Launcher.getConfig().clientPort));
        ClientProfile profile = params.profile;
        Launcher.profile = profile;
        AuthService.profile = profile;
        LauncherEngine.clientParams = params;
        Request.setSession(params.session);
        checkJVMBitsAndVersion();
        LauncherEngine.modulesManager.invokeEvent(new ClientProcessInitPhase(engine, params));

        Path clientDir = Paths.get(params.clientDir);
        Path assetDir = Paths.get(params.assetDir);

        // Verify ClientLauncher sign and classpath
        LogHelper.debug("Verifying ClientLauncher sign and classpath");
        List<URL> classpath = new LinkedList<>();
        resolveClassPathStream(clientDir, params.profile.getClassPath()).map(IOHelper::toURL).collect(Collectors.toCollection(() -> classpath));

        params.profile.pushOptionalClassPath((opt) -> {
            resolveClassPathStream(clientDir, opt).map(IOHelper::toURL).collect(Collectors.toCollection(() -> classpath));
        });
        classLoader = new ClientClassLoader(classpath.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        classLoader.nativePath = clientDir.resolve("natives").toString();
        LauncherEngine.modulesManager.invokeEvent(new ClientProcessClassLoaderEvent(engine, classLoader, profile));
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
        AuthService.username = params.playerProfile.username;
        AuthService.uuid = params.playerProfile.uuid;
        ClientService.classLoader = classLoader;
        ClientService.nativePath = classLoader.nativePath;
        classLoader.addURL(IOHelper.getCodeSource(ClientLauncherEntryPoint.class).toUri().toURL());
        //classForName(classLoader, "com.google.common.collect.ForwardingMultimap");
        ClientService.baseURLs = classLoader.getURLs();
        LauncherEngine.modulesManager.invokeEvent(new ClientProcessReadyEvent(engine, params));
        LogHelper.debug("Starting JVM and client WatchService");
        FileNameMatcher assetMatcher = profile.getAssetUpdateMatcher();
        FileNameMatcher clientMatcher = profile.getClientUpdateMatcher();
        try (DirWatcher assetWatcher = new DirWatcher(assetDir, params.assetHDir, assetMatcher, digest);
             DirWatcher clientWatcher = new DirWatcher(clientDir, params.clientHDir, clientMatcher, digest)) {
            // Verify current state of all dirs
            //verifyHDir(IOHelper.JVM_DIR, jvmHDir.object, null, digest);
            //for (OptionalFile s : Launcher.profile.getOptional()) {
            //    if (params.updateOptional.contains(s)) s.mark = true;
            //    else hdir.removeR(s.file);
            //}
            Launcher.profile.pushOptionalFile(params.clientHDir, false);
            // Start WatchService, and only then client
            CommonHelper.newThread("Asset Directory Watcher", true, assetWatcher).start();
            CommonHelper.newThread("Client Directory Watcher", true, clientWatcher).start();
            verifyHDir(assetDir, params.assetHDir, assetMatcher, digest);
            verifyHDir(clientDir, params.clientHDir, clientMatcher, digest);
            LauncherEngine.modulesManager.invokeEvent(new ClientProcessLaunchEvent(engine, params));
            launch(profile, params);
        }
    }

    private static void initGson(ClientModuleManager moduleManager) {
        Launcher.gsonManager = new ClientGsonManager(moduleManager);
        Launcher.gsonManager.initGson();
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

    private static LinkedList<Path> resolveClassPathList(Path clientDir, String... classPath) throws IOException {
        return resolveClassPathStream(clientDir, classPath).collect(Collectors.toCollection(LinkedList::new));
    }

    private static Stream<Path> resolveClassPathStream(Path clientDir, String... classPath) throws IOException {
        Stream.Builder<Path> builder = Stream.builder();
        for (String classPathEntry : classPath) {
            Path path = clientDir.resolve(IOHelper.toPath(classPathEntry.replace(IOHelper.CROSS_SEPARATOR, IOHelper.PLATFORM_SEPARATOR)));
            if (IOHelper.isDir(path)) { // Recursive walking and adding
                IOHelper.walk(path, new ClassPathFileVisitor(builder), false);
                continue;
            }
            builder.accept(path);
        }
        return builder.build();
    }

    private static void launch(ClientProfile profile, ClientLauncherProcess.ClientParams params) throws Throwable {
        // Add client args
        Collection<String> args = new LinkedList<>();
        if (profile.getVersion().compareTo(ClientProfile.Version.MC164) >= 0)
            params.addClientArgs(args);
        else {
            params.addClientLegacyArgs(args);
            System.setProperty("minecraft.applet.TargetDirectory", params.clientDir);
        }
        Collections.addAll(args, profile.getClientArgs());
        profile.pushOptionalClientArgs(args);
        List<String> copy = new ArrayList<>(args);
        for (int i = 0, l = copy.size(); i < l; i++) {
            String s = copy.get(i);
            if (i + 1 < l && ("--accessToken".equals(s) || "--session".equals(s))) {
                copy.set(i + 1, "censored");
            }
        }
        LogHelper.debug("Args: " + copy);
        // Resolve main class and method
        Class<?> mainClass = classLoader.loadClass(profile.getMainClass());
        for (URL u : classLoader.getURLs()) {
            LogHelper.info("ClassLoader URL: %s", u.toString());
        }
        FMLPatcher.apply();
        LauncherEngine.modulesManager.invokeEvent(new ClientProcessPreInvokeMainClassEvent(params, profile, args));
        MethodHandle mainMethod = MethodHandles.publicLookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class)).asFixedArity();
        Launcher.LAUNCHED.set(true);
        JVMHelper.fullGC();
        // Invoke main method
        try {
            mainMethod.invokeWithArguments((Object) args.toArray(new String[0]));
            LogHelper.debug("Main exit successful");
        } catch (Throwable e) {
            LogHelper.error(e);
            throw e;
        } finally {
            LauncherEngine.exitLauncher(0);
        }

    }

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
}
