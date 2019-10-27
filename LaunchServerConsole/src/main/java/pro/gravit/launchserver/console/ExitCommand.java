package pro.gravit.launchserver.console;

import pro.gravit.utils.command.Command;

public class ExitCommand extends Command {
    public ExitCommand() {
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return null;
    }

    @Override
    public void invoke(String... args) {
        System.exit(0);
    }
}
