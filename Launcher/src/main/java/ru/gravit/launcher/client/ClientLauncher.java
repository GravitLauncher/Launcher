package ru.gravit.launcher.client;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import javax.swing.JOptionPane;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

import ru.gravit.launcher.*;
import ru.gravit.launcher.hasher.DirWatcher;
import ru.gravit.launcher.hasher.FileNameMatcher;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.utils.PublicURLClassLoader;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.EnvHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.JVMHelper.OS;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.update.LauncherRequest;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launcher.serialize.stream.StreamObject;

public final class ClientLauncher {
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
        public final Set<ClientProfile.MarkedString> updateOptional;
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
        private final byte[] launcherSign;

        @LauncherAPI
        public Params(byte[] launcherSign, Path assetDir, Path clientDir, PlayerProfile pp, String accessToken,
                      boolean autoEnter, boolean fullScreen, int ram, int width, int height) {
            this.launcherSign = launcherSign.clone();
            this.updateOptional = new HashSet<>();
            for(ClientProfile.MarkedString s : Launcher.profile.getOptional())
            {
                if(s.mark) updateOptional.add(s);
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
        }

        @LauncherAPI
        public Params(HInput input) throws Exception {
            launcherSign = input.readByteArray(-SecurityHelper.RSA_KEY_LENGTH);
            // Client paths
            assetDir = IOHelper.toPath(input.readString(0));
            clientDir = IOHelper.toPath(input.readString(0));
            updateOptional = new HashSet<>();
            int len = input.readLength(128);
            for(int i=0;i<len;++i)
            {
                updateOptional.add(new ClientProfile.MarkedString(input.readString(512),true));
            }
            // Client params
            pp = new PlayerProfile(input);
            byte[] encryptedAccessToken = input.readByteArray(SecurityHelper.CRYPTO_MAX_LENGTH);
            String accessTokenD = new String(SecurityHelper.decrypt(Launcher.getConfig().secretKeyClient.getBytes(),encryptedAccessToken));
            accessToken = SecurityHelper.verifyToken(accessTokenD);
            autoEnter = input.readBoolean();
            fullScreen = input.readBoolean();
            ram = input.readVarInt();
            width = input.readVarInt();
            height = input.readVarInt();
        }

        @Override
        public void write(HOutput output) throws IOException {
            output.writeByteArray(launcherSign, -SecurityHelper.RSA_KEY_LENGTH);
            // Client paths
            output.writeString(assetDir.toString(), 0);
            output.writeString(clientDir.toString(), 0);
            output.writeLength(updateOptional.size(),128);
            for(ClientProfile.MarkedString s : updateOptional)
            {
                output.writeString(s.string,512);
            }
            // Client params
            pp.write(output);
            try {
                output.writeByteArray(SecurityHelper.encrypt(Launcher.getConfig().secretKeyClient.getBytes(),accessToken.getBytes()), SecurityHelper.CRYPTO_MAX_LENGTH);
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
    private static final int SOCKET_PORT = 32148;
    private static final String MAGICAL_INTEL_OPTION = "-XX:HeapDumpPath=ThisTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump";
    private static final boolean isUsingWrapper = true;
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
                JsonObject properties = Json.object();
                if (pp.skin != null) {
                    properties.add(Launcher.SKIN_URL_PROPERTY, Json.array(pp.skin.url));
                    properties.add(Launcher.SKIN_DIGEST_PROPERTY, Json.array(SecurityHelper.toHex(pp.skin.digest)));
                }
                if (pp.cloak != null) {
                    properties.add(Launcher.CLOAK_URL_PROPERTY, Json.array(pp.cloak.url));
                    properties.add(Launcher.CLOAK_DIGEST_PROPERTY, Json.array(SecurityHelper.toHex(pp.cloak.digest)));
                }
                Collections.addAll(args, "--userProperties", properties.toString(WriterConfig.MINIMAL));

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

        // Add window size args
        if (params.fullScreen)
            Collections.addAll(args, "--fullscreen", Boolean.toString(true));
        if (params.width > 0 && params.height > 0) {
            Collections.addAll(args, "--width", Integer.toString(params.width));
            Collections.addAll(args, "--height", Integer.toString(params.height));
        }
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
            JOptionPane.showMessageDialog(null, error);
        }
        String jvmVersion = JVMHelper.RUNTIME_MXBEAN.getVmVersion();
        LogHelper.info(jvmVersion);
        if (jvmVersion.startsWith("10.") || jvmVersion.startsWith("9.")) {
            String error = String.format("У Вас установлена Java %s. Для правильной работы необходима Java 8", JVMHelper.RUNTIME_MXBEAN.getVmVersion());
            LogHelper.error(error);
            JOptionPane.showMessageDialog(null, error);
        }
    }

    @LauncherAPI
    public static boolean isLaunched() {
        return Launcher.LAUNCHED.get();
    }

    @LauncherAPI
    public static boolean isUsingWrapper() {
        return JVMHelper.OS_TYPE == OS.MUSTDIE && isUsingWrapper;
    }

    private static void launch(ClientProfile profile, Params params) throws Throwable {
        // Add natives path
        //JVMHelper.addNativePath(params.clientDir.resolve(NATIVES_DIR));

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

    @LauncherAPI
    public static Process launch(

            SignedObjectHolder<HashedDir> assetHDir, SignedObjectHolder<HashedDir> clientHDir,
            SignedObjectHolder<ClientProfile> profile, Params params, boolean pipeOutput) throws Throwable {
        // Write params file (instead of CLI; Mustdie32 API can't handle command line > 32767 chars)
        LogHelper.debug("Writing ClientLauncher params");
        CommonHelper.newThread("Client params writter", false, () ->
        {
            try {
                try (ServerSocket socket = new ServerSocket()) {

                        socket.setReuseAddress(true);
                        socket.bind(new InetSocketAddress(SOCKET_HOST, SOCKET_PORT));
                        Socket client = socket.accept();
                        try (HOutput output = new HOutput(client.getOutputStream())) {
                            params.write(output);
                            profile.write(output);
                            assetHDir.write(output);
                            clientHDir.write(output);
                        }


                }
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }).start();
        // Resolve java bin and set permissions
        LogHelper.debug("Resolving JVM binary");
        //Path javaBin = IOHelper.resolveJavaBin(jvmDir);
        checkJVMBitsAndVersion();
        // Fill CLI arguments
        List<String> args = new LinkedList<>();
        boolean wrapper = isUsingWrapper();
        Path javaBin;
        if (wrapper) javaBin = JVMHelper.JVM_BITS == 64 ? AvanguardStarter.wrap64 : AvanguardStarter.wrap32;
        else
            javaBin = Paths.get(System.getProperty("java.home") + IOHelper.PLATFORM_SEPARATOR + "bin" + IOHelper.PLATFORM_SEPARATOR + "java");
        args.add(javaBin.toString());
        args.add(MAGICAL_INTEL_OPTION);
        if (params.ram > 0 && params.ram <= JVMHelper.RAM) {
            args.add("-Xms" + params.ram + 'M');
            args.add("-Xmx" + params.ram + 'M');
        }
        args.add(JVMHelper.jvmProperty(LogHelper.DEBUG_PROPERTY, Boolean.toString(LogHelper.isDebugEnabled())));
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
        StringBuilder classPathString = new StringBuilder(pathLauncher);
        LinkedList<Path> classPath = resolveClassPathList(params.clientDir, profile.object.getClassPath());
        for (Path path : classPath)
            classPathString.append(File.pathSeparatorChar).append(path.toString());
        Collections.addAll(args, profile.object.getJvmArgs());
        Collections.addAll(args, "-Djava.library.path=".concat(params.clientDir.resolve(NATIVES_DIR).toString())); // Add Native Path
        Collections.addAll(args, "-javaagent:".concat(pathLauncher));
        //Collections.addAll(args, "-classpath", classPathString.toString());
        //if(wrapper)
        //Collections.addAll(args, "-Djava.class.path=".concat(classPathString.toString())); // Add Class Path
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
        return builder.start();
    }

    @LauncherAPI
    public static void main(String... args) throws Throwable {
        Launcher.modulesManager = new ClientModuleManager(null);
        LauncherConfig.getAutogenConfig().initModules(); //INIT
        Launcher.modulesManager.preInitModules();
        if (JVMHelper.OS_TYPE == OS.MUSTDIE) {
            AvanguardStarter.loadVared();
            AvanguardStarter.main(false);
        }
        checkJVMBitsAndVersion();
        JVMHelper.verifySystemProperties(ClientLauncher.class, true);
        LogHelper.printVersion("Client Launcher");
        // Read and delete params file
        LogHelper.debug("Reading ClientLauncher params");
        Params params;
        SignedObjectHolder<ClientProfile> profile;
        SignedObjectHolder<HashedDir> assetHDir, clientHDir;
        RSAPublicKey publicKey = Launcher.getConfig().publicKey;
        try {
            try (Socket socket = IOHelper.newSocket()) {
                socket.connect(new InetSocketAddress(SOCKET_HOST, SOCKET_PORT));
                try (HInput input = new HInput(socket.getInputStream())) {
                    params = new Params(input);
                    profile = new SignedObjectHolder<>(input, publicKey, ClientProfile.RO_ADAPTER);

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
        Launcher.profile = profile.object;
        Launcher.modulesManager.initModules();
        // Verify ClientLauncher sign and classpath
        LogHelper.debug("Verifying ClientLauncher sign and classpath");
        SecurityHelper.verifySign(LauncherRequest.BINARY_PATH, params.launcherSign, publicKey);
        LinkedList<Path> classPath = resolveClassPathList(params.clientDir, profile.object.getClassPath());
        for (Path classpathURL : classPath) {
            LauncherAgent.addJVMClassPath(classpathURL.toAbsolutePath().toString());
        }
        URL[] classpathurls = resolveClassPath(params.clientDir, profile.object.getClassPath());
        classLoader = new PublicURLClassLoader(classpathurls, ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        PublicURLClassLoader.systemclassloader = classLoader;
        // Start client with WatchService monitoring
        boolean digest = !profile.object.isUpdateFastCheck();
        LogHelper.debug("Starting JVM and client WatchService");
        FileNameMatcher assetMatcher = profile.object.getAssetUpdateMatcher();
        FileNameMatcher clientMatcher = profile.object.getClientUpdateMatcher();
        try (DirWatcher assetWatcher = new DirWatcher(params.assetDir, assetHDir.object, assetMatcher, digest);
             DirWatcher clientWatcher = new DirWatcher(params.clientDir, clientHDir.object, clientMatcher, digest)) {
            // Verify current state of all dirs
            //verifyHDir(IOHelper.JVM_DIR, jvmHDir.object, null, digest);
            HashedDir hdir = clientHDir.object;
            for(ClientProfile.MarkedString s : Launcher.profile.getOptional())
            {
                if(params.updateOptional.contains(s)) s.mark = true;
                else hdir.removeR(s.string);
            }
            verifyHDir(params.assetDir, assetHDir.object, assetMatcher, digest);
            verifyHDir(params.clientDir, hdir, clientMatcher, digest);
            Launcher.modulesManager.postInitModules();
            // Start WatchService, and only then client
            CommonHelper.newThread("Asset Directory Watcher", true, assetWatcher).start();
            CommonHelper.newThread("Client Directory Watcher", true, clientWatcher).start();
            launch(profile.object, params);
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

    @LauncherAPI
    public static void setProfile(ClientProfile profile) {
        Launcher.profile = profile;
        LogHelper.debug("New Profile name: %s", profile.getTitle());
    }

    @LauncherAPI
    public static void verifyHDir(Path dir, HashedDir hdir, FileNameMatcher matcher, boolean digest) throws IOException {
        if (matcher != null)
            matcher = matcher.verifyOnly();

        // Hash directory and compare (ignore update-only matcher entries, it will break offline-mode)
        HashedDir currentHDir = new HashedDir(dir, matcher, false, digest);
        if (!hdir.diff(currentHDir, matcher).isSame())
            throw new SecurityException(String.format("Forbidden modification: '%s'", IOHelper.getFileName(dir)));
    }

    private ClientLauncher() {
    }
}

// It's here since first commit, there's no any reasons to remove :D
// ++oyyysssssssssssooooooo++++++++/////:::::-------------................----:::----::+osssso+///+++++ooys/:/+ssssyyssyooooooo+++////:::::::::-::///++ossyhdddddddhhys/----::::::::::::::/:////////////
// ++oyyssssssssssoooooooo++++++++//////:::::--------------------------------:::::-:::/+oo+//://+oo+//syysssssyyyyyhyyyssssssoo+++///::--:----:--:://++osyyhdddmddmdhys/------:::::::::::::::///////////
// ++syyssssssssssoooooooo++++++++///////:::::::::::::::-----------------------::--::/++++/:--::/+++//osysshhhhyhhdyyyyyssyssoo++//::-------------::/+++oyhyhdddmddmdhy+--------::::::::::::::://///////
// ++ssssssssssssooooooooo++++++///////::::::::-------------------------------------::/+//:----://+//+oyhhhddhhdhhhyhyyyyhysoo+//::----------------://++oshhhhmddmddmhhs:---------:::::::::::::::///////
// ++sssssssssssooooooooo++++////::::::::::------------------------------------:---:::///:-----://++osyhddddhdddhhhhhyyhdysso+/::-------------------://+ooyhhddmddmdmdhy/----------::::::::::::::://////
// ++sssssssssoooooooo+++////::::::::::-------------------------------------::///::::///::--::/++osyhhddmmdhddddhhhhhyhdhyso+/:----------------------::/++oyhdhdmmmmmddho-------------::::::::::::://///
// /+ssssssssosooo+++/////::::::::::----------------------------------:::::://+++ooossso////++oosyhddddmmdhddddhhhhhhhdhyddhs+:-----------------------::/++syhddmmmmdddho--------------::::::::::::::///
// /+sssssssooo+++//////::::::::::-------------------------------:::://++++ooooooooooooo+/+oosyyhhddddmmddddddhhhhhhhdhyyssyyhs+::-------------::://++/+/o++shddddmmmddh+----------------::::::::::::://
// /+sssooso++++/////::::::::::::------------------------::::::///++o++//:--...............-/osyyhddmmmmddddhhhhhyyhdhhdmddhysoso::--------::/osyyyssyyssssoosdmddddddy+:-------------------:::::::::::/
// /+sssooso+++////::::::::::::--------------------:::::////++o+//:-......................--+ossydddmmdmmdddhhhhyyhddyyydmhhooo++/:-------::/+oshhhyso+/://++ohmmmdhhy+:---------------------:::::::::::
// /+sooooso++/////:::::::::::--------------:::::///+++oo+//:-............................-/+oyydddmmddddddhhhyyyhhhyysosss+:/+///:--.---://+o/ohhdhoyyo+/://+ymmdhsyo:------------------------:::::::::
// /+ooooooso+////:::::::::------------:::::///++oooo/:-..................................:/oyyyhdmdddddddhhhhyyhyss++///:::/::::/:-...--::://-/shy+-:+s+/::/+hdhysos/--------------------------::::::::
// /+oooooooo++///::::::::::-:----:::::::///+oosyy/-.....................................-/+syhhddddddddhhhhhhhysso+/::::-----:::::-..------:::::////:::::::/sdysso+o:..----.--------------------:::::::
// /+ooooooooo++///:::::::::::::::::::///++osyhmmdo:-...................................-/+syyhdddddddddhdddmmhys+/::----------:::--..---------::::::-----::+dhsoso++:....--.--..----------------:::::::
// /ooooooooooo++///::::::://///++oooosssyydddhdhs+so/+++:-............................./+syyhdddddddddddddmmmhs+//:------------:--..---------------------:/hdsooso+/-....--.....------------------:::::
// /oooooooo++++o++//:::::::///++ossso+ooosysos++o/so//////--..........................:yhddhdddmmdddmmmdddmddhs+/::----------:::---.--------------------:/hdhsooso+/-.....-........----------------::::
// /ooooooo+++++++++//:::::::::/+o+ooooyyso+oo/++o/oo///://////::--...................-sdhddhddmmdhdmmmddhhdmNds+/:::--------:/:------------------------::sdhyooyso+:-...............---------------::::
// /ooooooo+++++++++++//:::://+++++ssyys+/+y+///++/so///:://://::::/---://............-smdddddmmhyhhdddhhsydmNmyo//:::------://:--------:::------------::odhysshyo++-................---------------::::
// /oooooo+++++++++++/+o+/++++++++ohhoo+//yo//:/+//so:/:////::+////+///+so/++/:--------:hdddmdhyssyhhddhsosdmNmho+/:::------////:------::::-----------::+hdhhhyyo+o/-................----------------:::
// /ooo++o++++++++++//+oooooo++++oyyoo+//os+////+//so/://::////+///+//+ssooo+++++++/++//odhhhyoooosyhhhysoodmmNds+//:::-----:++o+/:::/+//:----------::/+ydhyyyyo+oo:...................--------------:::
// /oooo+++++++++++/////+ossoooooss++////+so/::++//os://:/::://////++/os+++++++/++++++++osyso++++osyyyyssoodmmmmyo+/::::-----::/++++////:--------::::/+shhysoso+ss/.....................------------:::/
// +ooo++++++++++/////////+oooosso////:::+oso/:/+//oy+//:///://////+//o+/++////////++++ooooo++++ossyhhsoosodmmmmds+//////::::::::::::---------::::://+shhyso+++ss:........................----------:://
// +ooo++++++++++///////////////++++//:::/++o////+/+yo//////////////:/o:/+//////////+oooo+oooo++ooyyyyysssshmmmmmds+//////+++++///:::::::::::::::://+syhhso+++oo/..........................---------::/+
// +o+o+++++++++///////////////::///++//:::/+///////oy+///://///////-++:+/://://::/++so++ooooooossysyyhhssyhmmmmNmds+//:::/++++oooooooo+++//:::::/+oosyys++oso+/-..........................--------:://+
// +oo+++++++++/////////////////:::://++//////:/+/:/+ss+//////++///:://///::://:://++o++ossssyyssosyyhhyssydmmmmNNmmyo//////++///////////::::::/+oooosossssyyo/-...........................--------://++
// +++++++++++//////////////::/::::::::///++/:///:://oss//:////++//::+/////:::/:://+so++ssyyhhhoosyyyysoosydmmmNNmmmmho+//::////+++++///:::::/+osssssshmdssyyo-............................-------::/+++
// +oo++++++++++/////////////:/::::::::::://+/////:/:+oso+:////++/://+++++++/::///+oooosyyyhhhsossssso+osshmmmmNmmmmmdys+/::::---:::::::::/+osyyyssyddmmhsyhy+-.............................-----:://+++
// +o++++++++++///////////:/::::::::::::---::/+++/::::+oso/::/://:://++++o+:::+//++ssooyyyhhyyssssoo++sssymmmmNmmmmmmhysso/:::::-----:::/+yhhhyyyhddmmdhhyhys+:-............................-----://+++/
// ++++++++++++/////////////::::::::----------://////:/+oso+/:::::://+++o+//+ooooo+++oshyhhhysssso++ososhmNNNNNmmmmhhyoooooo+/////://+oyhmmdhhyhdddddhyyysssoo+:--.........................-----://++//:
// o++++++++++/////////////:::::::::-------------:////::/ooo/:::::///+os++ssyyyyssssssyhhhysssssoooooosdmNNNNNmmmdhsss+++ooooooossyyhdmmmmdhyysyhhyssooooossooys+//:--.....................----::/+++/::
// o+++++++++//////////////::::::::----------.....-://////+oo+:://://ooooyhhhhdddhhhyhhhhyssssssssooshmNNNNNNNmmmdooo++//++oooossshddmmmddys++ooo+/+++ossyhhyyyyysoo+//:--.................----://++/:::
// ++++++++++//////////////:::::::--------..........-::///:/+++::::/+o+/ohhhhhhyyyyyyhhhysyyysyyyyhdmNNNNNNNNmmmhso++//:///+ooossydddmmmddhyysso+/++ossshddddddhyyso++++////::::::--.......---://++/:-::
// o+++++++++///////////////:::::::------..............-:////++/:///+osoyhhhhyssssyhhdddhhhyyhhdmmmmmmmmmmmmmmmho+oo+/::::///+++oyhhdmmmddhhysoo+oosyhhdmmmddddhhyssoo+++++++++++///:-....---://++//-:::
// +++++++++//+///////////:/::::::------.................-://////////+ossyyyyyhhhhddddhhhhddmmmmddddmmmmmmmmmdyo+++os/::::::://+ydhhmmmddddddddddmmmmmmmmmmmmddhyyysoo+o+++//++////::::-----::/++//--::+
// +++++++++///////////////:::::::------....................-:///++/::+syyyhhhhhhhhhhhhddmmmddddddddmmmmmmmmdso++++++++/:::://ohdhydmmddddhdmmmmmmhhdmmmmmmmmmhhyyyysooso+/+////////////:::::/+++/:-::++
// ++++++++++//////////////:/:::::------......................-:/+/:::+shhdhhhhhhhhhdddmmmddddhhhhhddmmmmmmdyo+++++/+/++++++oydmdhyhdddhhsssydmdmmddddddmmmmmdhhsyysoso++++///+//////://+//////+/:-::/o:
// ++++++++++/////////////://:::::------.........................::::/osdmmmmmddddddddddhhysssooosyhdmmmmmdsooo+o++/+++++++shmmddhyyhmhso/+shhdmddhhhdmmmmddhhyyysoooo//+++/://///:/+//+///////::-::/o/:
// +++++++++++////////////:/::::::-----..........................-:://+oymmmmmddddhhhhhhhhyysoo+osyhhdmdhyoo/++so++++oooooohmmmmdhhyhdh+//+ydhyssshddmmdhhhyyssso+/+o+//+////:////+//+////+////+++////:-
// +++++++++//////////////://:::::-----.........................-::::///oyddddhhhhhdmdddhddhhysssyhhdmdyo++o/+/+oooo+++o+osdmmmmdhhyhdd+/+syo++oshddhyyhhyssooo+///+/++/+++/://:/+/++++o++oooooso++oo++/
// o+++++++++///////////////::::::-----.........................::::::++o+shmdyhhsyhdddmmddddhsosyhhmmh++/+o+/+/++++ooososhmddddhyyso++oosso++osyyhhddmdysooo+//+/o+++/+/o//o+/++++/+ooosssooooo+++++/++
// o+++++++++///////////////:::::------........................-::::::+ossyyhyo+++shddmmdyyhyo//osysdds//++++o+/++o+o++oosdhdddhhyyysoooysoosyyyhdddhmmss+o+o++//+/+++//+++++oo++/oossssssssoooooooo++++
// o++++++++++/////////////:/::::-----.........................:::::/oyysyysooo+oyhdddmdhhddh+++osyhdh//////++o+///++ooooyshdmmmmdddhdhssshhysydmmysshyoo++++++/+++////+o+ooso+++ssyyyyyyyyysssooooo+o++
// ooo+++++++++//////////////::::-----.........................:-:::ohdhhhhysssssyhhhyhhhdmmd+++oyddho////:/++++++/+/+++oshmmmmmdhyyyssyhdysshmmmdhyysoo+/+o+//++/+//++ososso++syhyyyhyyyyyyyysssysssyss
// ooo+++++++++////////////::::::------.......................---::+hddhhyysoosoooosssooosyyyssyhdhho+///////////+/+++++symmmdys+//+oyhhyssyhdmmmmmhyo+++++++/+++++oo++ohssoooyhhhhhhhyyyhyhhyyhyyhyyyyy
// ooo++++++/++/////////////::::::-----.......................:::/:/+sssyyysssyo++oosyhhhhyyssoo+oooo+///////////+++++oyhddhysssyyhdddhysoshmNNNNmmyoo+++++++/+/++++o+osdyssoyhhhhhhhhhhhhhhyhhhhhhhhhhh
// oooo++++++++/////////////::::::-----.......................:::::///++ooooo++/:/+syhhddys++/:/+++o+++////++////+++osysydhydmddddyshysoshmmmmmNNmyooo+oo+++++/++oooooshdyoyyhhhhhhhhhhdhhhhhdhhhhdhhhhd
// ooo+++++++++/////////////::::::----.......................-://::::://////:///://ooooo+////////++oo++++////++//oosyhyyyyshddmmhyhyysyhmNmdddmNdysooooosooooooooooso+shdysyhdhhhhhhhdddhdhdddddddhhhddd
// oooo+++++++++////////////::::::----.......................::/:::::///////://::::::::::://:/::/++oo+++////++++oohyhhhhhhsydmdhhhyyhhyymmmdddmmyssoooosysossosoo+ssossddysydhhddhhddhhddddddddddddhdddd
// oooo+++++++++/++//////////::::-----......................-:::::::///////::/:::::::::::::::::/:/++oo++++++/++ssyhhddddhyyohmddhhddhyhhmmmddmmdsssoosssosssossooossoyhmhyyhdhddddhhhdhhddddddddhddddmdd
// oooo+++++++++//+//////////::::----.......................-:::::///:/::::::/::::-::::/:-:::::///++ooooo+++++shhdddddhhyyhysdddhdmdhhddmdhhydmhssosssysssssssoosssssyhmysyddddddhdhdddddddddddddddmdddd
// ooooo+++++++++++//////////::::----.......................::::::://://:::://::-:::-::::::::::////++oosoo+o+oyhhdddddhhhhddyyhyhddhhhdmmmhyyddhssssssyyysssssssssysyyddyyhddddddddhhhhhddddddddmddddddm
// ooooo+o++++++++++/////////::::----.......................::::::/:///::::::::::::::::::::/:::::///++osssososssyhddddhhhhdddssoossyhdmmmmhyyhhhyssyyyyysyyyssyssssshhmhyhdddddddddddhdhhddmddmdddddhdmm
// ooooooo++++++++++/////////:::-----......................-:::::/::::/:/::://::::/::::::::::::::/:://+ooosssyysyyyyyyyhddmmysssssyyhhddmdhysyhhyyyyyyyysyyyyysssyysyydhydddddddhddddhddddddddddddhddmmm
// ooooooo+++++++++++////////:::-----......................:::::/:::://:-::/:://::/::::-:::::::::/:/::/++oosssyyysssssyhdddhsyysssyyhhddhhyssyyyyyyyyysyyssyyyyyyyyyhhdyhdddhddddhddddddmmddddddhdddmmmm
// ooooooo+o+++++++++++//////:::-----.....................-::::::/:///:::::::::/::/::::::::::/::/:///::////+oosyysssyssyyyysssyhyyhddmmdyysoossyssyyysssyyssysyyyyshyddhddhhhddhdddddddddmddhdddhhdhhdmm
// ooooooooo++++++++++///////:::-----.....................-::::////:::::::::::::::::::://::::/-:::::/:://::///+osyyssosossssssysyhdddddhysoosssyyssssyssyyyyyysssyshddddddhddddddddddmddddhdhhhddddmmmmm
// oooooooooo+o++++++++++////::::----.....................::://::::/::::::::::::::/:://::::::::::/::::::///////++ooooosoo+ososssshddhhysssooossssssssyssysyyyyyysysyhddddddddddddddddddddhddhhhddddmmmmd
// soooooooooo+++++++++++////::::----....................-:::::////:::/:::::::/::://///:/::::::::/:///://:/://+//+++++++oo+ooooooshdyyyssossoosssyssyysssssyyyyysssshdddddddddddddddddddhhhdhhhddmmmmdmd
// sssooooooooo++++++++++////::::----....................-:::/:://///:::::::::::::/://:/::://::::::://:::/://///++/+++/+++o+oosssyyysssyysosoososysssyysyyyyhysyssyyhdddddddddddddddddhddhddhddmmmmmdmmm
// ssssoooooooo++++++++++////:::::----...................:::::://::::::::::::-::://///:////::/:/:::://///:://://+o++++++oooosoosoossoososssooososssssyyyyyssyyyssyyyhmmdddhhhhhhhddhdddddddddmmmmmmddmmm
// sssoooooooooooo+++++++/////::::----...................:://///:/:::::::::::::::://///////////+/:/://://::////+++++++ooossoossososososssoososossssssyssyyyyyyyyyysyhdhhhhhhhhhhhdhhhhhdddddmmmmmmddmmNN
// ssssoooooooooooo+++++++////::::----..................-::::/:/:::::::::::::://:::://///:/:///////////:://+/++++++oooooooosssssoosssssssosooosooossssyyysyyysyyyysoyyooossssyyhhhddhddhhhhhddmmmmmmmmmm
// ssssssoooooooooooo++++++///:::------.................::::::://:::::::-:::::/::://////:///////////////////++o++oo+ooooosoossossosssossssoosoosoosssyysyyyysyyssysoyo+oooo//+osossosyhhhhhhddmmddmmmmdm
// sssssssosooooooooooo++++///::::-----................./::::/:/:::::::::-::/:::/://///////////+//++/++//+oo+oo++ooo+ooooosssosoossssysosssssooooossysyyyyysyyyyyssoo++ooo+//+s/:---::/+oosyyyhhhhdmmmmm
// yssssssssssoooooooo+++++///::::-----................://::::::/:/::::::-::/::::://///////////////+/++++++oo+++oooooo+oooosssossosysosssosssoooosssyyyyyyyyyyyysso++++++//:/o/:--::::::::::/++oossymmmm
// yysssssssssssooooooo++++////:::------..............-///:::/:::/:::/::::://:::::::///////////////++++++o++++o++oo+ooooooooosysosssssoosssssosoossyyyyyssyyyyysss++oooo+///oo/::::-::::::::::::////sdmm
// yyysssssssssssooooooo++++///:::-------.............:/:::/:/:::::::::://://::/::::/:////////+/////++++++oo+ooo++oooo+oooossssssssssoossoooooossssyyyyyyyyyyyysso++ooo/--:/:.::::..::::-..-::://////ohm
// yyyssssssssssssoooooo++++///::::------............-/:::/://///:::::::::://::/::::::////:///+/+++/++++++/++++++o+o++oooooooosssssoossssssssosssssyyyyyyyyyyyyso++ooo:./.-+-.---...--:::.--::::://////o
// yyyysyssssssssssoooooo+++///::::--------.........-/:::::////::/::::::::::/:://:/:::///////////+/+++++//++++++++oo+++ooosoooossssssossoosssssssssyyyyyyyyyyysoo+oooo`:+.+.--.--.-::.-.----::-:.///////
// yyyyyyyyyssssssssooooo+++///:::::----------......:///:::://///:::::::::::://:/:::://///////+////++++////+++++++++o++oooosssososossossososssssysyyysyyyyyyysso+oooo+--:-+-.-.-------::----:--:-://////
// yyyyyyyyyyssssssssoooo++++//:::::-------------.--////://::/:::::/::::/:::://:/::::/:/:///////+//+++///+//++/++++o++++ooooooosssossssosoossssssssssyyyyyyyyso++ooso++/o+/::::::::::::::://////////////
// yyyyyyyyyyyssssssssoooo+++///::::---------------:////////////:/:///::::://////::::::://///////+/++////+//+/++++++++ooosooooooososoosososssosyyyssyysyyyyysso+oso+//+oo/:::::::::::::/:::///////////++
// yyyyyyyyyyyyysssssssooo+++///:::::---------------+////:///:///:::::///::://///:::/:::///////////++/++++///++++/++o+oooooooooosoososoossssssssysssyyyyyyysoo+ooso+++ss/::::::::::::::://///////////+++
// hhyyyyyyyyyyyyyssssssooo++///:::::---------------/////////:////://:///::://///://::://////////++////+///+///++o++++ooooooooooooooooooooososososssyssssysooo+oso+++os+:::::///////////////////////++++
// hhhyyyyyyyyyyyyyyssssooo+++///::::---------------:////////////:////////:://///::://////////++/+++//////+++++++++++++++oo+++o+ooososooosssoossssssssssssooo+osso+oos+:::////////////////////////++++++
// hhhhhhyyyyyyyyyyyyssssoo+++////:::::--------------:///////////:///://///:///////:///://///++/++++//++/++++/++++++++++++++o+oooooooosoosssssoosssssssssso++ossooooyo/:///////////////////////++++++++o
