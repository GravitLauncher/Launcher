package pro.gravit.launchserver.command.hash;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public final class DownloadAssetCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public DownloadAssetCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[version] [dir]";
    }

    @Override
    public String getUsageDescription() {
        return "Download asset dir";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        //Version version = Version.byName(args[0]);
        String versionName = args[0];
        String dirName = IOHelper.verifyFileName(args[1]);
        Path assetDir = server.updatesDir.resolve(dirName);

        // Create asset dir
        logger.info("Creating asset dir: '{}'", dirName);
        Files.createDirectory(assetDir);

        // Download required asset
        logger.info("Downloading asset, it may take some time");
        //HttpDownloader.downloadZip(server.mirrorManager.getDefaultMirror().getAssetsURL(version.name), assetDir);
        server.mirrorManager.downloadZip(assetDir, "assets/%s.zip", versionName);

        // Finished
        server.syncUpdatesDir(Collections.singleton(dirName));
        logger.info("Asset successfully downloaded: '{}'", dirName);
    }
}
