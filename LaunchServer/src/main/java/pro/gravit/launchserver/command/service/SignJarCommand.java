package pro.gravit.launchserver.command.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.SignJarTask;
import pro.gravit.launchserver.command.Command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class SignJarCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public SignJarCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[path to file] (path to signed file)";
    }

    @Override
    public String getUsageDescription() {
        return "sign custom jar";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        Path target = Paths.get(args[0]);
        Path tmpSign;
        if (args.length > 1)
            tmpSign = Paths.get(args[1]);
        else
            tmpSign = server.dir.resolve("build").resolve(target.toFile().getName());
        logger.info("Signing jar {} to {}", target.toString(), tmpSign.toString());
        Optional<SignJarTask> task = server.launcherBinary.getTaskByClass(SignJarTask.class);
        if (task.isEmpty()) throw new IllegalStateException("SignJarTask not found");
        task.get().sign(server.config.sign, target, tmpSign);
        if (args.length <= 1) {
            logger.info("Move temp jar {} to {}", tmpSign.toString(), target.toString());
            Files.deleteIfExists(target);
            Files.move(tmpSign, target);
        }
        logger.info("Success signed");
    }
}
