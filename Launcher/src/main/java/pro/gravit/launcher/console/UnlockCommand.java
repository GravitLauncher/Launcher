package pro.gravit.launcher.console;

import pro.gravit.launcher.managers.ConsoleManager;
import pro.gravit.launcher.managers.SettingsManager;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class UnlockCommand extends Command {
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
            LogHelper.info("Unlock successful");
            if (!ConsoleManager.unlock()) {
                LogHelper.error("Console unlock canceled");
                return;
            }
            LogHelper.info("Write unlock key");
            SettingsManager.settings.consoleUnlockKey = args[0];
        } else {
            LogHelper.error("Unlock key incorrect");
        }
    }
}
