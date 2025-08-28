package pro.gravit.launchserver.command.hash;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.helper.IOHelper;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class UnindexAssetCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public UnindexAssetCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[dir] [index] [output-dir]";
    }

    @Override
    public String getUsageDescription() {
        return "Unindex asset dir (1.7.10+)";
    }

    @Override
    public void invoke(String... args) throws Exception {
        /*verifyArgs(args, 3);
        String inputAssetDirName = IOHelper.verifyFileName(args[0]);
        String indexFileName = IOHelper.verifyFileName(args[1]);
        String outputAssetDirName = IOHelper.verifyFileName(args[2]);
        var updatesDir = server.config.updatesProvider.getUpdatesDir(inputAssetDirName);
        if(updatesDir == null) {
            server.config.updatesProvider.create(inputAssetDirName);
        }
        Path outputAssetDir = Path.of(outputAssetDirName);

        // Create new asset dir
        logger.info("Creating unindexed asset dir: '{}'", outputAssetDirName);
        Files.createDirectory(outputAssetDir);

        // Read JSON file
        JsonObject objects;
        logger.info("Reading asset index file: '{}'", indexFileName);
        Path indexFilePath = IndexAssetCommand.resolveIndexFile(Path.of(""), indexFileName);
        try (BufferedReader reader = IOHelper.newReader(server.config.updatesProvider.download(inputAssetDirName, indexFilePath.toString()))) {
            objects = JsonParser.parseReader(reader).getAsJsonObject().get("objects").getAsJsonObject();
        }

        // Restore objects
        logger.info("Unindexing {} objects", objects.size());
        for (Map.Entry<String, JsonElement> member : objects.entrySet()) {
            String name = member.getKey();
            logger.info("Unindexing: '{}'", name);

            // Copy hashed file to target
            String hash = member.getValue().getAsJsonObject().get("hash").getAsString();
            Path source = IndexAssetCommand.resolveObjectFile(Path.of(""), hash);
            server.config.updatesProvider.download(inputAssetDirName, Map.of(source.toString(), outputAssetDir.resolve(name)));
        }

        // Finished
        server.syncUpdatesDir(Collections.singleton(outputAssetDirName));
        logger.info("Asset successfully unindexed: '{}'", outputAssetDir.toAbsolutePath().toString());*/
    }
}
