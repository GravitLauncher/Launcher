package pro.gravit.launcher.base;

import pro.gravit.launcher.core.managers.GsonManager;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.core.serialize.HInput;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public final class Launcher {

    // Authlib constants


    // Used to determine from clientside is launched from launcher
    public static final AtomicBoolean LAUNCHED = new AtomicBoolean(false);
    public static final String RUNTIME_DIR = "runtime";

    // Constants
    public static final String CONFIG_FILE = "config.bin";
    private static final AtomicReference<LauncherConfig> CONFIG = new AtomicReference<>();
    private static final Pattern UUID_PATTERN = Pattern.compile("-", Pattern.LITERAL);
    public static ClientProfile profile;
    public static GsonManager gsonManager;


    public static LauncherConfig getConfig() {
        LauncherConfig config = CONFIG.get();
        if (config == null) {
            try (HInput input = new HInput(IOHelper.newInput(IOHelper.getResourceURL(CONFIG_FILE)))) {
                config = new LauncherConfig(input);
            } catch (IOException | InvalidKeySpecException e) {
                throw new SecurityException(e);
            }
            CONFIG.set(config);
        }
        return config;
    }


    public static void setConfig(LauncherConfig cfg) {
        CONFIG.set(cfg);
    }


    public static URL getResourceURL(String name) throws IOException {
        LauncherConfig config = getConfig();
        byte[] validDigest = config.runtime.get(name);
        if (validDigest == null)
            throw new NoSuchFileException(name);

        // Resolve URL and verify digest
        URL url = IOHelper.getResourceURL(RUNTIME_DIR + '/' + name);

        // Return verified URL
        return url;
    }

    public static URL getResourceURL(String name, String prefix) throws IOException {
        LauncherConfig config = getConfig();
        byte[] validDigest = config.runtime.get(name);
        if (validDigest == null)
            throw new NoSuchFileException(name);

        // Resolve URL and verify digest
        URL url = IOHelper.getResourceURL(prefix + '/' + name);

        // Return verified URL
        return url;
    }


    public static String toHash(UUID uuid) {
        return UUID_PATTERN.matcher(uuid.toString()).replaceAll("");
    }

    public static void applyLauncherEnv(LauncherConfig.LauncherEnvironment env) {
        switch (env) {
            case DEV:
                LogHelper.setDevEnabled(true);
                LogHelper.setStacktraceEnabled(true);
                LogHelper.setDebugEnabled(true);
                break;
            case DEBUG:
                LogHelper.setDebugEnabled(true);
                LogHelper.setStacktraceEnabled(true);
                break;
            case STD:
                break;
            case PROD:
                LogHelper.setStacktraceEnabled(false);
                LogHelper.setDebugEnabled(false);
                LogHelper.setDevEnabled(false);
                break;
        }
    }

    public static String makeSpecialGuardDirName(JVMHelper.ARCH arch, JVMHelper.OS os) {
        return String.format("%s-%s", arch.name, os.name);
    }
}
