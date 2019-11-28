package pro.gravit.launcher.client;

import pro.gravit.launcher.ClientLauncherWrapper;
import pro.gravit.launcher.guard.LauncherGuardManager;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.managers.ClientHookManager;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.utils.helper.EnvHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static pro.gravit.launcher.client.ClientLauncher.checkJVMBitsAndVersion;

public class ClientProcessBuilder {
    private HashedDir assetHDir;
    private HashedDir clientHDir;
    private ClientProfile profile;
    private ClientLauncher.Params params;
    private Path nativesDir = IOHelper.toPath("natives");
    private Path resourcepacksDir = IOHelper.toPath("resourcepacks");
    private boolean pipeOutput = false;
    private boolean clientLaunchStarting = false;
    public interface ParamsWriter
    {
        void write(ClientLauncherContext context);
    }
    private ParamsWriter paramsWriter;

    private static final String MAGICAL_INTEL_OPTION = "-XX:HeapDumpPath=ThisTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump";

    public ClientProcessBuilder setAssetHDir(HashedDir assetHDir) {
        this.assetHDir = assetHDir;
        return this;
    }

    public ClientProcessBuilder setClientHDir(HashedDir clientHDir) {
        this.clientHDir = clientHDir;
        return this;
    }

    public ClientProcessBuilder setProfile(ClientProfile profile) {
        this.profile = profile;
        return this;
    }

    public ClientProcessBuilder setParams(ClientLauncher.Params params) {
        this.params = params;
        return this;
    }

    public ClientProcessBuilder setNativesDir(Path nativesDir) {
        this.nativesDir = nativesDir;
        return this;
    }

    public ClientProcessBuilder setResourcepacksDir(Path resourcepacksDir) {
        this.resourcepacksDir = resourcepacksDir;
        return this;
    }

    public ClientProcessBuilder setPipeOutput(boolean pipeOutput) {
        this.pipeOutput = pipeOutput;
        return this;
    }

    public ClientProcessBuilder setParamsWriter(ParamsWriter paramsWriter) {
        this.paramsWriter = paramsWriter;
        return this;
    }

    public ClientLauncherContext build() throws IOException {
        LogHelper.debug("Writing ClientLauncher params");
        ClientLauncherContext context = new ClientLauncherContext();
        clientLaunchStarting = true;
        checkJVMBitsAndVersion();
        LogHelper.debug("Resolving JVM binary");
        Path javaBin = LauncherGuardManager.getGuardJavaBinPath();
        context.javaBin = javaBin;
        context.clientProfile = profile;
        context.playerProfile = params.pp;
        context.args.add(javaBin.toString());
        context.args.add(MAGICAL_INTEL_OPTION);
        context.params = params;
        if(paramsWriter != null) paramsWriter.write(context);
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
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
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
        Collections.addAll(context.args, "-Djava.library.path=".concat(params.clientDir.resolve(nativesDir).toString())); // Add Native Path
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
            builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        }
        List<String> command = builder.command();
        // Let's rock!
        ClientHookManager.preStartHook.hook(context, builder);
        context.process = builder.start();
        if (builder.command() != command) {
            LogHelper.error("Something strange cheating...");
            System.exit(100);
            return null;
        }
        ClientHookManager.postStartHook.hook(context, builder);
        return context;
    }
}
