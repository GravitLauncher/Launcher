package pro.gravit.launchserver.binary.tasks.exe;

import net.sf.launch4j.Builder;
import net.sf.launch4j.Log;
import net.sf.launch4j.config.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.Path;

public class Launch4JTask implements LauncherBuildTask, BuildExeMainTask {
    public static final String DOWNLOAD_URL = "http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html"; // Oracle
    private static final String VERSION = Version.getVersion().getVersionString();
    private static final int BUILD = Version.getVersion().build;
    private final Path faviconFile;
    private final LaunchServer server;
    private transient final Logger logger = LogManager.getLogger();

    public Launch4JTask(LaunchServer launchServer) {
        this.server = launchServer;
        faviconFile = launchServer.dir.resolve("favicon.ico");
    }

    public static String formatVars(String mask) {
        return String.format(mask, VERSION, BUILD);
    }

    @Override
    public String getName() {
        return "launch4j";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        logger.info("Building launcher EXE binary file (Using Launch4J)");
        Path output = setConfig();

        // Set favicon path
        Config config = ConfigPersister.getInstance().getConfig();
        if (IOHelper.isFile(faviconFile))
            config.setIcon(faviconFile.toFile());
        else {
            config.setIcon(null);
            logger.warn("Missing favicon.ico file");
        }

        // Start building
        Builder builder = new Builder(Launch4JLog.INSTANCE);
        try {
            builder.build();
        } catch (Throwable e) {
            throw new IOException(e);
        }
        return output;
    }

    @Override
    public boolean allowDelete() {
        return true;
    }

    private Path setConfig() {
        Path path = server.launcherEXEBinary.nextPath(getName());
        Config config = new Config();
        // Set file options
        config.setChdir(".");
        config.setErrTitle("JVM Error");
        config.setDownloadUrl(server.config.launch4j.downloadUrl);
        if (server.config.launch4j.supportURL != null) config.setSupportUrl(server.config.launch4j.supportURL);
        // Set boolean options
        config.setPriorityIndex(0);
        config.setHeaderType(Config.GUI_HEADER);
        config.setStayAlive(false);
        config.setRestartOnCrash(false);

        // Prepare JRE
        Jre jre = new Jre();
        jre.setMinVersion(server.config.launch4j.minVersion);
        if (server.config.launch4j.setMaxVersion)
            jre.setMaxVersion(server.config.launch4j.maxVersion);
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
        info.setOriginalFilename(path.getFileName().toString());
        info.setLanguage(LanguageID.RUSSIAN);
        config.setVersionInfo(info);

        // Set JAR wrapping options
        config.setDontWrapJar(false);
        config.setJar(server.launcherBinary.syncBinaryFile.toFile());
        config.setOutfile(path.toFile());

        // Return prepared config
        ConfigPersister.getInstance().setAntConfig(config, null);
        return path;
    }

    private final static class Launch4JLog extends Log {
        private static final Launch4JLog INSTANCE = new Launch4JLog();
        private static final Logger logger = LogManager.getLogger();

        @Override
        public void append(String s) {
            logger.info(s);
        }

        @Override
        public void clear() {
            // Do nothing
        }
    }
}
