package pro.gravit.launchserver.command.hash;

import com.google.gson.JsonElement;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;

public final class DownloadClientCommand extends Command {

    public DownloadClientCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[version] [dir]";
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

        // Create client dir
        LogHelper.subInfo("Creating client dir: '%s'", dirName);
        Files.createDirectory(clientDir);

        // Download required client
        LogHelper.subInfo("Downloading client, it may take some time");
        //HttpDownloader.downloadZip(server.mirrorManager.getDefaultMirror().getClientsURL(version.name), clientDir);
        server.mirrorManager.downloadZip(clientDir, "clients/%s.zip", versionName);

        // Create profile file
        LogHelper.subInfo("Creaing profile file: '%s'", dirName);
        ClientProfile client;
        String profilePath = String.format("pro/gravit/launchserver/defaults/profile%s.cfg", versionName);
        try (BufferedReader reader = IOHelper.newReader(IOHelper.getResourceURL(profilePath))) {
            client = Launcher.gsonManager.configGson.fromJson(reader, ClientProfile.class);
        } catch (IOException e) {
            JsonElement clientJson = server.mirrorManager.jsonRequest(null, "GET", "clients/%s.json", versionName);
            client = Launcher.gsonManager.configGson.fromJson(clientJson, ClientProfile.class);
        }
        client.setTitle(dirName);
        client.setDir(dirName);
        client.setUUID(UUID.randomUUID());
        if (client.getServers() != null) {
            ClientProfile.ServerProfile serverProfile = client.getDefaultServerProfile();
            if (serverProfile != null) {
                serverProfile.name = dirName;
            }
        }
        try (BufferedWriter writer = IOHelper.newWriter(IOHelper.resolveIncremental(server.profilesDir,
                dirName, "json"))) {
            Launcher.gsonManager.configGson.toJson(client, writer);
        }

        // Finished
        server.syncProfilesDir();
        server.syncUpdatesDir(Collections.singleton(dirName));
        LogHelper.subInfo("Client successfully downloaded: '%s'", dirName);
    }
}
