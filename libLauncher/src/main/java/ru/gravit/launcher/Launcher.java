package ru.gravit.launcher;

import java.io.IOException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import ru.gravit.launcher.modules.ModulesManagerInterface;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.utils.Version;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.SecurityHelper;

public final class Launcher {

    // Authlib constants
    @LauncherAPI
    public static final String SKIN_URL_PROPERTY = "skinURL";
    @LauncherAPI
    public static final String SKIN_DIGEST_PROPERTY = "skinDigest";
    @LauncherAPI
    public static final String CLOAK_URL_PROPERTY = "cloakURL";
    @LauncherAPI
    public static final String CLOAK_DIGEST_PROPERTY = "cloakDigest";
    // Used to determine from clientside is launched from launcher
    public static final AtomicBoolean LAUNCHED = new AtomicBoolean(false);

    private static final AtomicReference<LauncherConfig> CONFIG = new AtomicReference<>();
    @LauncherAPI
    public static ModulesManagerInterface modulesManager = null;
    @LauncherAPI
    public static final int PROTOCOL_MAGIC_LEGACY = 0x724724_00 + 24;
    @LauncherAPI
    public static final int PROTOCOL_MAGIC = 0xA205B064; // e = 2.718281828

    // Constants
    @LauncherAPI
    public static final String RUNTIME_DIR = "runtime";
    @LauncherAPI
    public static final String GUARD_DIR = "guard";
    @LauncherAPI
    public static final String CONFIG_FILE = "config.bin";
    @LauncherAPI
    public static ClientProfile profile;
    @LauncherAPI
    public static final String INIT_SCRIPT_FILE = "init.js";
    @LauncherAPI
    public static final String API_SCRIPT_FILE = "engine/api.js";
    public static final String CONFIG_SCRIPT_FILE = "config.js";

    private static final Pattern UUID_PATTERN = Pattern.compile("-", Pattern.LITERAL);
    public static int MAJOR = 4;
    public static int MINOR = 1;
    public static int PATCH = 0;
    public static int BUILD = 2;
    public static Version.Type RELEASE = Version.Type.DEV;

    @LauncherAPI
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

    @LauncherAPI
    public static void setConfig(LauncherConfig cfg) {
        CONFIG.set(cfg);
    }

    @LauncherAPI
    public static URL getResourceURL(String name) throws IOException {
        LauncherConfig config = getConfig();
        byte[] validDigest = config.runtime.get(name);
        if (validDigest == null)
            throw new NoSuchFileException(name);

        // Resolve URL and verify digest
        URL url = IOHelper.getResourceURL(RUNTIME_DIR + '/' + name);
        if (!Arrays.equals(validDigest, SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, url)))
            throw new NoSuchFileException(name); // Digest mismatch

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
        if (!Arrays.equals(validDigest, SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, url)))
            throw new NoSuchFileException(name); // Digest mismatch

        // Return verified URL
        return url;
    }

    @LauncherAPI
    public static String toHash(UUID uuid) {
        return UUID_PATTERN.matcher(uuid.toString()).replaceAll("");
    }

    public static Version getVersion() {
        return new Version(MAJOR, MINOR, PATCH, BUILD, RELEASE);
    }

    public static final boolean useAvanguard = true;

    public static boolean isUsingAvanguard() {
        return JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE && useAvanguard;
    }
}
