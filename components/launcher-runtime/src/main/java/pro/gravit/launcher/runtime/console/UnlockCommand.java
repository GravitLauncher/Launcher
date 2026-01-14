package pro.gravit.launcher.runtime.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.runtime.managers.ConsoleManager;
import pro.gravit.launcher.runtime.managers.SettingsManager;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class UnlockCommand extends Command {

    private static final Logger logger =
            LoggerFactory.getLogger(UnlockCommand.class);

    @Override
    public String getArgsDescription() {
        return "[key]";
    }

    @Override
    public String getUsageDescription() {
        return "Unlock console commands";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        if (ConsoleManager.checkUnlockKey(args[0])) {
            logger.info("Unlock successful");
            if (!ConsoleManager.unlock()) {
                logger.error("Console unlock canceled");
                return;
            }
            logger.info("Write unlock key");
            SettingsManager.settings.consoleUnlockKey = args[0];
        } else {
            logger.error("Unlock key incorrect");
        }
    }
}