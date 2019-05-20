package ru.gravit.utils.command.basic;

import ru.gravit.utils.command.Command;
import ru.gravit.utils.command.CommandHandler;
import ru.gravit.utils.helper.LogHelper;

public final class ClearCommand extends Command {
    private CommandHandler handler;

    public ClearCommand(CommandHandler handler) {
        this.handler = handler;
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Clear terminal";
    }

    @Override
    public void invoke(String... args) throws Exception {
        handler.clear();
        LogHelper.subInfo("Terminal cleared");
    }
}
