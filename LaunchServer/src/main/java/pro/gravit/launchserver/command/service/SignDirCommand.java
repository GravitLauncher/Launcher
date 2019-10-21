package pro.gravit.launchserver.command.service;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.SignJarTask;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class SignDirCommand extends Command {
    private class SignJarVisitor extends SimpleFileVisitor<Path>
    {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toFile().getName().endsWith(".jar"))
            {
                Path tmpSign = server.dir.resolve("build").resolve(file.toFile().getName());
                LogHelper.info("Signing jar %s", file.toString());
                SignJarTask.sign(server.config.sign, file, tmpSign);
                Files.deleteIfExists(file);
                Files.move(tmpSign, file);
            }
            return super.visitFile(file, attrs);
        }
    }
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
        if(!IOHelper.isDir(targetDir))
            throw new IllegalArgumentException(String.format("%s not directory", targetDir.toString()));
        IOHelper.walk(targetDir, new SignJarVisitor(), true);
        LogHelper.info("Success signed");
    }
}
