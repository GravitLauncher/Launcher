package ru.gravit.launchserver.command.hash;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.SecurityHelper.DigestAlgorithm;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.launchserver.command.CommandException;

public final class IndexAssetCommand extends Command {
    private static final class IndexAssetVisitor extends SimpleFileVisitor<Path> {
        private final JsonObject objects;
        private final Path inputAssetDir;
        private final Path outputAssetDir;

        private IndexAssetVisitor(JsonObject objects, Path inputAssetDir, Path outputAssetDir) {
            this.objects = objects;
            this.inputAssetDir = inputAssetDir;
            this.outputAssetDir = outputAssetDir;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String name = IOHelper.toString(inputAssetDir.relativize(file));
            LogHelper.subInfo("Indexing: '%s'", name);

            // Add to index and copy file
            String digest = SecurityHelper.toHex(SecurityHelper.digest(DigestAlgorithm.SHA1, file));
            objects.add(name, Json.object().add("size", attrs.size()).add("hash", digest));
            IOHelper.copy(file, resolveObjectFile(outputAssetDir, digest));

            // Continue visiting
            return super.visitFile(file, attrs);
        }
    }

    public static final String INDEXES_DIR = "indexes";
    public static final String OBJECTS_DIR = "objects";

    private static final String JSON_EXTENSION = ".json";


    public static Path resolveIndexFile(Path assetDir, String name) {
        return assetDir.resolve(INDEXES_DIR).resolve(name + JSON_EXTENSION);
    }


    public static Path resolveObjectFile(Path assetDir, String hash) {
        return assetDir.resolve(OBJECTS_DIR).resolve(hash.substring(0, 2)).resolve(hash);
    }

    public IndexAssetCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "<dir> <index> <output-dir>";
    }

    @Override
    public String getUsageDescription() {
        return "Index asset dir (1.7.10+)";
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
            throw new CommandException("Unindexed and indexed asset dirs can't be same");

        // Create new asset dir
        LogHelper.subInfo("Creating indexed asset dir: '%s'", outputAssetDirName);
        Files.createDirectory(outputAssetDir);

        // Index objects
        JsonObject objects = Json.object();
        LogHelper.subInfo("Indexing objects");
        IOHelper.walk(inputAssetDir, new IndexAssetVisitor(objects, inputAssetDir, outputAssetDir), false);

        // Write index file
        LogHelper.subInfo("Writing asset index file: '%s'", indexFileName);
        try (BufferedWriter writer = IOHelper.newWriter(resolveIndexFile(outputAssetDir, indexFileName))) {
            Json.object().add(OBJECTS_DIR, objects).writeTo(writer, WriterConfig.MINIMAL);
        }

        // Finished
        server.syncUpdatesDir(Collections.singleton(outputAssetDirName));
        LogHelper.subInfo("Asset successfully indexed: '%s'", inputAssetDirName);
    }
}
