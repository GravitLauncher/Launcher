package pro.gravit.launchserver.command.hash;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launchserver.HttpRequester;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DownloadAssetCommand extends Command {
    private static final String MINECRAFT_VERSIONS_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String RESOURCES_DOWNLOAD_URL = "https://resources.download.minecraft.net/";
    private transient final Logger logger = LogManager.getLogger();

    public DownloadAssetCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[version] [dir] (mojang/mirror)";
    }

    @Override
    public String getUsageDescription() {
        return "Download asset dir";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        //Version version = Version.byName(args[0]);
        String versionName = args[0];
        String dirName = IOHelper.verifyFileName(args.length > 1 ? args[1] : "assets");
        String type = args.length > 2 ? args[2] : "mojang";
        Path assetDir = server.updatesDir.resolve(dirName);

        // Create asset dir
        if (Files.notExists(assetDir)) {
            logger.info("Creating asset dir: '{}'", dirName);
            Files.createDirectory(assetDir);
        }

        if (type.equals("mojang")) {
            HttpRequester requester = new HttpRequester();
            logger.info("Fetch versions from {}", MINECRAFT_VERSIONS_URL);
            var versions = requester.send(requester.get(MINECRAFT_VERSIONS_URL, null), MinecraftVersions.class).getOrThrow();
            String profileUrl = null;
            for (var e : versions.versions) {
                if (e.id.equals(versionName)) {
                    profileUrl = e.url;
                    break;
                }
            }
            if (profileUrl == null) {
                logger.error("Version {} not found", versionName);
                return;
            }
            logger.info("Fetch profile {} from {}", versionName, profileUrl);
            var profileInfo = requester.send(requester.get(profileUrl, null), MiniVersion.class).getOrThrow();
            String assetsIndexUrl = profileInfo.assetIndex.url;
            String assetIndex = profileInfo.assetIndex.id;
            Path indexPath = assetDir.resolve("indexes").resolve(assetIndex + ".json");
            logger.info("Fetch asset index {} from {}", assetIndex, assetsIndexUrl);
            JsonObject assets = requester.send(requester.get(assetsIndexUrl, null), JsonObject.class).getOrThrow();
            JsonObject objects = assets.get("objects").getAsJsonObject();
            try (Writer writer = IOHelper.newWriter(indexPath)) {
                logger.info("Save {}", indexPath);
                Launcher.gsonManager.configGson.toJson(assets, writer);
            }
            if (!assetIndex.equals(versionName)) {
                Path targetPath = assetDir.resolve("indexes").resolve(versionName + ".json");
                logger.info("Copy {} into {}", indexPath, targetPath);
                Files.copy(indexPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            List<Downloader.SizedFile> toDownload = new ArrayList<>(128);
            for (var e : objects.entrySet()) {
                var value = e.getValue().getAsJsonObject();
                var hash = value.get("hash").getAsString();
                hash = hash.substring(0, 2) + "/" + hash;
                var size = value.get("size").getAsLong();
                var path = "objects/" + hash;
                var target = assetDir.resolve(path);
                if (Files.exists(target)) {
                    long fileSize = Files.size(target);
                    if (fileSize != size) {
                        logger.warn("File {} corrupted. Size {}, expected {}", target, size, fileSize);
                    } else {
                        continue;
                    }
                }
                toDownload.add(new Downloader.SizedFile(hash, path, size));
            }
            logger.info("Download {} files", toDownload.size());
            Downloader downloader = downloadWithProgressBar(dirName, toDownload, RESOURCES_DOWNLOAD_URL, assetDir);
            downloader.getFuture().get();
        } else {
            // Download required asset
            logger.info("Downloading asset, it may take some time");
            //HttpDownloader.downloadZip(server.mirrorManager.getDefaultMirror().getAssetsURL(version.name), assetDir);
            server.mirrorManager.downloadZip(assetDir, "assets/%s.zip", versionName);
        }

        // Finished
        server.syncUpdatesDir(Collections.singleton(dirName));
        logger.info("Asset successfully downloaded: '{}'", dirName);
    }

    public record MiniVersionInfo(String id, String url) {

    }

    public record MinecraftVersions(List<MiniVersionInfo> versions) {

    }

    public record MinecraftAssetIndexInfo(String id, String url) {

    }

    public record MiniVersion(MinecraftAssetIndexInfo assetIndex) {

    }
}
