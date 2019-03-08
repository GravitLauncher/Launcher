package ru.gravit.launchserver.command.hash;

import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.profiles.ClientProfile.Version;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.command.CommandException;
import ru.gravit.utils.HttpDownloader;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public final class DownloadClientCommand extends Command {

    public DownloadClientCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "<version> <dir>";
    }

    @Override
    public String getUsageDescription() {
        return "Download client dir";
    }

    @Override
    public void invoke(String... args) throws IOException, CommandException {
        verifyArgs(args, 2);
        Version version = Version.byName(args[0]);
        String dirName = IOHelper.verifyFileName(args[1]);
        Path clientDir = server.updatesDir.resolve(args[1]);

        // Create client dir
        LogHelper.subInfo("Creating client dir: '%s'", dirName);
        Files.createDirectory(clientDir);

        // Download required client
        LogHelper.subInfo("Downloading client, it may take some time");
        HttpDownloader.downloadZip(server.mirrorManager.getDefaultMirror().getClientsURL(version.name), clientDir);

        // Create profile file
        LogHelper.subInfo("Creaing profile file: '%s'", dirName);
        ClientProfile client;
        String profilePath = String.format("ru/gravit/launchserver/defaults/profile%s.cfg", version.name);
        try (BufferedReader reader = IOHelper.newReader(IOHelper.getResourceURL(profilePath))) {
            client = LaunchServer.gson.fromJson(reader, ClientProfile.class);
        }
        client.setTitle(dirName);
        client.setDir(dirName);
        try (BufferedWriter writer = IOHelper.newWriter(IOHelper.resolveIncremental(server.profilesDir,
                dirName, "cfg"))) {
            LaunchServer.gson.toJson(client, writer);
        }

        // Finished
        server.syncProfilesDir();
        server.syncUpdatesDir(Collections.singleton(dirName));
        LogHelper.subInfo("Client successfully downloaded: '%s'", dirName);
    }
}
