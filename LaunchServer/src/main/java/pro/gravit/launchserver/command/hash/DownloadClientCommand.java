package pro.gravit.launchserver.command.hash;

import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.helper.MakeProfileHelper;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.helper.IOHelper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;

public final class DownloadClientCommand extends Command {

    private transient final Logger logger = LogManager.getLogger();

    public DownloadClientCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[version] [dir] (mirror/generate)";
    }

    @Override
    public String getUsageDescription() {
        return "Download client dir";
    }

    @Override
    public void invoke(String... args) throws IOException, CommandException {
        verifyArgs(args, 2);
        //Version version = Version.byName(args[0]);
        String versionName = args[0];
        String dirName = IOHelper.verifyFileName(args[1]);
        Path clientDir = server.updatesDir.resolve(args[1]);

        boolean isMirrorClientDownload = false;
        if (args.length > 2) {
            isMirrorClientDownload = args[2].equals("mirror");
        }

        // Create client dir
        logger.info("Creating client dir: '{}'", dirName);
        Files.createDirectory(clientDir);

        // Download required client
        logger.info("Downloading client, it may take some time");
        //HttpDownloader.downloadZip(server.mirrorManager.getDefaultMirror().getClientsURL(version.name), clientDir);
        server.mirrorManager.downloadZip(clientDir, "clients/%s.zip", versionName);

        // Create profile file
        logger.info("Creaing profile file: '{}'", dirName);
        ClientProfile client = null;
        if (!isMirrorClientDownload) {
            try {
                String internalVersion = versionName;
                if (internalVersion.contains("-")) {
                    internalVersion = internalVersion.substring(0, versionName.indexOf('-'));
                }
                ClientProfile.Version version = ClientProfile.Version.byName(internalVersion);
                if (version.compareTo(ClientProfile.Version.MC164) <= 0) {
                    logger.warn("Minecraft 1.6.4 and below not supported. Use at your own risk");
                }
                MakeProfileHelper.MakeProfileOption[] options = MakeProfileHelper.getMakeProfileOptionsFromDir(clientDir, version);
                for (MakeProfileHelper.MakeProfileOption option : options) {
                    logger.debug("Detected option {}", option.getClass().getSimpleName());
                }
                client = MakeProfileHelper.makeProfile(version, dirName, options);
            } catch (Throwable e) {
                isMirrorClientDownload = true;
            }
        }
        if (isMirrorClientDownload) {
            JsonElement clientJson = server.mirrorManager.jsonRequest(null, "GET", "clients/%s.json", versionName);
            client = Launcher.gsonManager.configGson.fromJson(clientJson, ClientProfile.class);
            client.setTitle(dirName);
            client.setDir(dirName);
            client.setUUID(UUID.randomUUID());
            if (client.getServers() != null) {
                ClientProfile.ServerProfile serverProfile = client.getDefaultServerProfile();
                if (serverProfile != null) {
                    serverProfile.name = dirName;
                }
            }
        }
        try (BufferedWriter writer = IOHelper.newWriter(IOHelper.resolveIncremental(server.profilesDir,
                dirName, "json"))) {
            Launcher.gsonManager.configGson.toJson(client, writer);
        }

        // Finished
        server.syncProfilesDir();
        server.syncUpdatesDir(Collections.singleton(dirName));
        logger.info("Client successfully downloaded: '{}'", dirName);
    }
}
