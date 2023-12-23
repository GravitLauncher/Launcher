package pro.gravit.launchserver.binary.tasks.exe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.Path;

public class Launch4JTask implements LauncherBuildTask, BuildExeMainTask {
    public static final String DOWNLOAD_URL = "https://bell-sw.com/pages/downloads/?version=java-8-lts&os=Windows&package=jre-full"; // BellSoft
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
        return mask.formatted(VERSION, BUILD);
    }

    @Override
    public String getName() {
        return "launch4j";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        throw new UnsupportedOperationException("Launch4j not supported");
    }
}
