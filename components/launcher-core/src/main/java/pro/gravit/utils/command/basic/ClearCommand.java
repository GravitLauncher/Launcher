package pro.gravit.utils.command.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.helper.LogHelper;

public final class ClearCommand extends Command {

    private static final Logger logger =
            LoggerFactory.getLogger(ClearCommand.class);

    private final CommandHandler handler;

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