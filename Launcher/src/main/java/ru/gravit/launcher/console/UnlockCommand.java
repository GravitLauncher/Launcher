package ru.gravit.launcher.console;

import ru.gravit.launcher.managers.ConsoleManager;
import ru.gravit.utils.command.Command;
import ru.gravit.utils.helper.LogHelper;

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
        if(ConsoleManager.checkUnlockKey(args[0]))
        {
            LogHelper.info("Unlock successful");
            ConsoleManager.unlock();
            ConsoleManager.handler.unregisterCommand("unlock");
        }
        else
        {
            LogHelper.error("Unlock key incorrect");
        }
    }
}
