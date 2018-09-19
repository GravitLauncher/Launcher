package ru.gravit.launcher;

import java.io.IOException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.launcher.modules.ModulesManagerInterface;
import ru.gravit.launcher.serialize.HInput;

public final class Launcher {
    private static final AtomicReference<LauncherConfig> CONFIG = new AtomicReference<>();
    // Version info
    //Все версии оригинального Sashok Launcher v3 считаются как 3.xx.xx, например 3.15.3 3.15.4
    //Все версии модификации считаются так: 3.16.xx Например 3.16.5 для коммита от 20 Августа
    @Deprecated
    @LauncherAPI
    public static final String VERSION = LauncherVersion.getVersion().getVersionString();
    @Deprecated
    @LauncherAPI
    public static final String BUILD = String.valueOf(LauncherVersion.readBuildNumber());
    //Начиная с 4.0.0 PROTOCOL_MAGIC изменит свою форму
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
    public static final String CONFIG_FILE = "config.bin";
    @LauncherAPI
    public static final String INIT_SCRIPT_FILE = "init.js";
    @LauncherAPI
    public static final String API_SCRIPT_FILE = "engine/api.js";

    private static final Pattern UUID_PATTERN = Pattern.compile("-", Pattern.LITERAL);

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

    @LauncherAPI
    @SuppressWarnings({"SameReturnValue", "MethodReturnAlwaysConstant"})
    public static String getVersion() {
        return VERSION; // Because Java constants are known at compile-time
    }

    @LauncherAPI
    public static String toHash(UUID uuid) {
        return UUID_PATTERN.matcher(uuid.toString()).replaceAll("");
    }

}
