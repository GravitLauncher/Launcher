package pro.gravit.launchserver.command.hash;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public final class DownloadAssetCommand extends Command {

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
        LogHelper.subInfo("Creating asset dir: '%s'", dirName);
        Files.createDirectory(assetDir);

        // Download required asset
        LogHelper.subInfo("Downloading asset, it may take some time");
        //HttpDownloader.downloadZip(server.mirrorManager.getDefaultMirror().getAssetsURL(version.name), assetDir);
        server.mirrorManager.downloadZip(assetDir, "assets/%s.zip", versionName);

        // Finished
        server.syncUpdatesDir(Collections.singleton(dirName));
        LogHelper.subInfo("Asset successfully downloaded: '%s'", dirName);
    }
}
