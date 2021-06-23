package pro.gravit.launchserver.command.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.SignJarTask;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

public class SignDirCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public SignDirCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[path to dir]";
    }

    @Override
    public String getUsageDescription() {
        return "sign all jar files into dir";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        Path targetDir = Paths.get(args[0]);
        if (!IOHelper.isDir(targetDir))
            throw new IllegalArgumentException(String.format("%s not directory", targetDir));
        Optional<SignJarTask> task = server.launcherBinary.getTaskByClass(SignJarTask.class);
        if (task.isEmpty()) throw new IllegalStateException("SignJarTask not found");
        IOHelper.walk(targetDir, new SignJarVisitor(task.get()), true);
        logger.info("Success signed");
    }

    private class SignJarVisitor extends SimpleFileVisitor<Path> {
        private final SignJarTask task;

        public SignJarVisitor(SignJarTask task) {
            this.task = task;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toFile().getName().endsWith(".jar")) {
                Path tmpSign = server.dir.resolve("build").resolve(file.toFile().getName());
                logger.info("Signing jar {}", file.toString());
                task.sign(server.config.sign, file, tmpSign);
                Files.deleteIfExists(file);
                Files.move(tmpSign, file);
            }
            return super.visitFile(file, attrs);
        }
    }
}
