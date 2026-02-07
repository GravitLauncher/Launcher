package pro.gravit.launchserver.command.profiles;

import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.launcher.base.profiles.ClientProfileVersions;
import pro.gravit.launchserver.HttpRequester;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.profiles.ProfilesProvider;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.helper.AssetsDirHelper;
import pro.gravit.launchserver.helper.MakeProfileHelper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CreateProfileCommand extends Command {

    private static final Logger logger = LogManager.getLogger();

    public CreateProfileCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[version] [dir] (mirror/generate)";
    }

    @Override
    public String getUsageDescription() {
        return "Download client";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        //Version version = Version.byName(args[0]);
        String versionName = args[0];
        String dirName = args[1];
        Path clientDir = server.createTempDirectory("client");

        boolean isMirrorProfileDownload = false;
        if (args.length > 2) {
            isMirrorProfileDownload = args[2].equals("mirror");
        }

        // Download required client
        logger.info("Downloading client, it may take some time");
        //HttpDownloader.downloadZip(server.mirrorManager.getDefaultMirror().getClientsURL(version.name), clientDir);
        server.mirrorManager.downloadZip(clientDir, "clients/%s.zip", versionName);

        // Create profile file
        logger.info("Creaing profile file: '{}'", dirName);
        ClientProfile clientProfile = null;
        try {
            JsonElement clientJson = server.mirrorManager.jsonRequest(null, "GET", "clients/%s.json", versionName);
            clientProfile = Launcher.gsonManager.configGson.fromJson(clientJson, ClientProfile.class);
            var builder = new ClientProfileBuilder(clientProfile);
            builder.setTitle(dirName);
            builder.setDir(dirName);
            builder.setUuid(UUID.randomUUID());
            clientProfile = builder.createClientProfile();
            if (clientProfile.getServers() != null) {
                ClientProfile.ServerProfile serverProfile = clientProfile.getDefaultServerProfile();
                if (serverProfile != null) {
                    serverProfile.name = dirName;
                }
            }
        } catch (Exception e) {
            logger.error("Filed download clientProfile from mirror: '{}' Generation through MakeProfileHelper", versionName);
            isMirrorProfileDownload = false;
        }
        if (!isMirrorProfileDownload) {
            try {
                String internalVersion = versionName;
                if (internalVersion.contains("-")) {
                    internalVersion = internalVersion.substring(0, versionName.indexOf('-'));
                }
                ClientProfile.Version version = ClientProfile.Version.of(internalVersion);
                if (version.compareTo(ClientProfileVersions.MINECRAFT_1_7_10) <= 0) {
                    logger.warn("Minecraft 1.7.9 and below not supported. Use at your own risk");
                }
                MakeProfileHelper.MakeProfileOption[] options = MakeProfileHelper.getMakeProfileOptionsFromDir(clientDir, version);
                for (MakeProfileHelper.MakeProfileOption option : options) {
                    logger.debug("Detected option {}", option.getClass().getSimpleName());
                }
                clientProfile = MakeProfileHelper.makeProfile(version, dirName, options);
            } catch (Throwable e) {
                isMirrorProfileDownload = true;
            }
        }
        pushClientAndDownloadAssets(server, clientProfile, clientDir, true);

        // Finished
        logger.info("Client successfully downloaded: '{}'", dirName);
    }

    public static ProfilesProvider.CompletedProfile pushClientAndDownloadAssets(LaunchServer server, ClientProfile clientProfile, Path clientDir, boolean isDownloadAssets) throws Exception {
        var uncompleted = server.config.profilesProvider.create(clientProfile.getTitle(), "Description", null);
        if(!isDownloadAssets) {
            return server.config.profilesProvider.pushUpdate(uncompleted, null, clientProfile, null, List.of(
                    ProfilesProvider.ProfileAction.upload(clientDir, "", true)
            ), List.of(ProfilesProvider.UpdateFlag.USE_DEFAULT_ASSETS));
        }
        var completed = server.config.profilesProvider.pushUpdate(uncompleted, null, clientProfile, null, List.of(
                ProfilesProvider.ProfileAction.upload(clientDir, "", true)
        ), List.of(ProfilesProvider.UpdateFlag.USE_DEFAULT_ASSETS));
        {
            String assetIndexPath = String.format("indexes/%s.json", completed.getProfile().getAssetIndex());
            if (!completed.getAssetDir().tryFindRecursive(assetIndexPath).isFound()) {
                Path assetDir = server.createTempDirectory("assets");
                HttpRequester requester = new HttpRequester();
                var assetInfo = AssetsDirHelper.getAssetInfo(requester, completed.getProfile().getAssetIndex());
                var toDownload = AssetsDirHelper.makeToDownloadFiles(assetInfo, completed.getAssetDir());
                logger.info("Download assets {}", completed.getProfile().getAssetIndex());
                Downloader downloader = downloadWithProgressBar(completed.getProfile().getAssetIndex(),
                        toDownload, AssetsDirHelper.RESOURCES_DOWNLOAD_URL, assetDir);
                downloader.getFuture().get();
                byte[] assetIndexBytes = Launcher.gsonManager.configGson.toJson(assetInfo.assets()).getBytes(StandardCharsets.UTF_8);
                completed = server.config.profilesProvider.pushUpdate(uncompleted, null, clientProfile, List.of(
                        ProfilesProvider.ProfileAction.upload(() -> {
                            return new ByteArrayInputStream(assetIndexBytes);
                        }, assetIndexPath),
                        ProfilesProvider.ProfileAction.upload(assetDir, "", true)
                ), List.of(
                        ProfilesProvider.ProfileAction.upload(clientDir, "", true)
                ), List.of(ProfilesProvider.UpdateFlag.USE_DEFAULT_ASSETS));
            } else {
                completed = server.config.profilesProvider.pushUpdate(uncompleted, null, clientProfile, null, List.of(
                        ProfilesProvider.ProfileAction.upload(clientDir, "", true)
                ), List.of(ProfilesProvider.UpdateFlag.USE_DEFAULT_ASSETS));
            }
        }
        return completed;
    }
}
