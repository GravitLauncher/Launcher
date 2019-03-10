package ru.gravit.launchserver.command.hash;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.command.CommandException;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public final class UnindexAssetCommand extends Command {
    private static JsonParser parser = new JsonParser();

    public UnindexAssetCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "<dir> <index> <output-dir>";
    }

    @Override
    public String getUsageDescription() {
        return "Unindex asset dir (1.7.10+)";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 3);
        String inputAssetDirName = IOHelper.verifyFileName(args[0]);
        String indexFileName = IOHelper.verifyFileName(args[1]);
        String outputAssetDirName = IOHelper.verifyFileName(args[2]);
        Path inputAssetDir = server.updatesDir.resolve(inputAssetDirName);
        Path outputAssetDir = server.updatesDir.resolve(outputAssetDirName);
        if (outputAssetDir.equals(inputAssetDir))
            throw new CommandException("Indexed and unindexed asset dirs can't be same");

        // Create new asset dir
        LogHelper.subInfo("Creating unindexed asset dir: '%s'", outputAssetDirName);
        Files.createDirectory(outputAssetDir);

        // Read JSON file
        JsonObject objects;
        LogHelper.subInfo("Reading asset index file: '%s'", indexFileName);
        try (BufferedReader reader = IOHelper.newReader(IndexAssetCommand.resolveIndexFile(inputAssetDir, indexFileName))) {
            objects = parser.parse(reader).getAsJsonObject().get("objects").getAsJsonObject();
        }

        // Restore objects
        LogHelper.subInfo("Unindexing %d objects", objects.size());
        for (Map.Entry<String, JsonElement> member : objects.entrySet()) {
            String name = member.getKey();
            LogHelper.subInfo("Unindexing: '%s'", name);

            // Copy hashed file to target
            String hash = member.getValue().getAsJsonObject().get("hash").getAsString();
            Path source = IndexAssetCommand.resolveObjectFile(inputAssetDir, hash);
            IOHelper.copy(source, outputAssetDir.resolve(name));
        }

        // Finished
        server.syncUpdatesDir(Collections.singleton(outputAssetDirName));
        LogHelper.subInfo("Asset successfully unindexed: '%s'", inputAssetDirName);
    }
}
