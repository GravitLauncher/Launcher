package ru.gravit.launchserver.binary;

import net.sf.launch4j.Builder;
import net.sf.launch4j.Log;
import net.sf.launch4j.config.*;
import ru.gravit.launcher.Launcher;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Path;

public final class EXEL4JLauncherBinary extends LauncherBinary {
    private final static class Launch4JLog extends Log {
        private static final Launch4JLog INSTANCE = new Launch4JLog();

        @Override
        public void append(String s) {
            LogHelper.subInfo(s);
        }

        @Override
        public void clear() {
            // Do nothing
        }
    }

    // URL constants
    private static final String DOWNLOAD_URL = "http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html"; // Oracle
    // JRE 8

    // File constants
    private final Path faviconFile;


    public EXEL4JLauncherBinary(LaunchServer server) {
        super(server, server.dir.resolve(server.config.binaryName + ".exe"));
        faviconFile = server.dir.resolve("favicon.ico");
    }

    @Override
    public void build() throws IOException {
        LogHelper.info("Building launcher EXE binary file (Using Launch4J)");
        setConfig();

        // Set favicon path
        Config config = ConfigPersister.getInstance().getConfig();
        if (IOHelper.isFile(faviconFile))
            config.setIcon(faviconFile.toFile());
        else {
            config.setIcon(null);
            LogHelper.warning("Missing favicon.ico file");
        }

        // Start building
        Builder builder = new Builder(Launch4JLog.INSTANCE);
        try {
            builder.build();
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    private void setConfig() {
        Config config = new Config();
        // Set file options
        config.setChdir(".");
        config.setErrTitle("JVM Error");
        config.setDownloadUrl(DOWNLOAD_URL);

        // Set boolean options
        config.setPriorityIndex(0);
        config.setHeaderType(Config.GUI_HEADER);
        config.setStayAlive(false);
        config.setRestartOnCrash(false);

        // Prepare JRE
        Jre jre = new Jre();
        jre.setMinVersion("1.8.0");
        jre.setRuntimeBits(Jre.RUNTIME_BITS_64_AND_32);
        jre.setJdkPreference(Jre.JDK_PREFERENCE_PREFER_JRE);
        config.setJre(jre);

        // Prepare version info (product)
        VersionInfo info = new VersionInfo();
        info.setProductName(server.config.launch4j.productName);
        info.setProductVersion(formatVars(server.config.launch4j.productVer));
        info.setFileDescription(server.config.launch4j.fileDesc);
        info.setFileVersion(formatVars(server.config.launch4j.fileVer));
        info.setCopyright(server.config.launch4j.copyright);
        info.setTrademarks(server.config.launch4j.trademarks);
        info.setInternalName(formatVars(server.config.launch4j.internalName));
        // Prepare version info (file)
        info.setTxtFileVersion(formatVars(server.config.launch4j.txtFileVersion));
        info.setTxtProductVersion(formatVars(server.config.launch4j.txtProductVersion));
        // Prepare version info (misc)
        info.setOriginalFilename(syncBinaryFile.getFileName().toString());
        info.setLanguage(LanguageID.RUSSIAN);
        config.setVersionInfo(info);

        // Set JAR wrapping options
        config.setDontWrapJar(false);
        config.setJar(server.launcherBinary.syncBinaryFile.toFile());
        config.setOutfile(syncBinaryFile.toFile());

        // Return prepared config
        ConfigPersister.getInstance().setAntConfig(config, null);
    }

    private static String VERSION = Launcher.getVersion().getVersionString();
    private static int BUILD = Launcher.getVersion().build;

    public static String formatVars(String mask) {
        return String.format(mask, VERSION, BUILD);
    }
}
