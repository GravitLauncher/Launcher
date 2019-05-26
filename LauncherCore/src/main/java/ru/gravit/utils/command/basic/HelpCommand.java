package ru.gravit.utils.command.basic;

import org.fusesource.jansi.Ansi;
import ru.gravit.utils.command.Command;
import ru.gravit.utils.command.CommandException;
import ru.gravit.utils.command.CommandHandler;
import ru.gravit.utils.helper.FormatHelper;
import ru.gravit.utils.helper.LogHelper;

import java.util.Map.Entry;

public final class HelpCommand extends Command {
    private CommandHandler handler;

    private static void printCommand(String name, Command command) {
        String args = command.getArgsDescription();
        //LogHelper.subInfo("%s %s - %s", name, args == null ? "[nothing]" : args, command.getUsageDescription());
        LogHelper.rawLog(() -> FormatHelper.rawFormat(LogHelper.Level.INFO, LogHelper.getDataTime(), true) + String.format("%s %s - %s", name, args == null ? "[nothing]" : args, command.getUsageDescription()), () -> {
            Ansi ansi = FormatHelper.rawAnsiFormat(LogHelper.Level.INFO, LogHelper.getDataTime(), true);
            ansi.fgBright(Ansi.Color.GREEN);
            ansi.a(name + " ");
            ansi.fgBright(Ansi.Color.CYAN);
            ansi.a(args == null ? "[nothing]" : args);
            ansi.reset();
            ansi.a(" - ");
            ansi.fgBright(Ansi.Color.YELLOW);
            ansi.a(command.getUsageDescription());
            ansi.reset();
            return ansi.toString();
        }, () -> LogHelper.htmlFormatLog(LogHelper.Level.INFO, LogHelper.getDataTime(), String.format("<font color=\"green\">%s</font> <font color=\"cyan\">%s</font> - <font color=\"yellow\">%s</font>", name, args == null ? "[nothing]" : args, command.getUsageDescription()), true));
    }

    private static void printCategory(String name, String description) {
        if (description != null) LogHelper.info("Category: %s - %s", name, description);
        else LogHelper.info("Category: %s", name);
    }

    public HelpCommand(CommandHandler handler) {
        this.handler = handler;
    }

    @Override
    public String getArgsDescription() {
        return "[command name]";
    }

    @Override
    public String getUsageDescription() {
        return "Print command usage";
    }

    @Override
    public void invoke(String... args) throws CommandException {
        if (args.length < 1) {
            printCommands();
            return;
        }

        // Print command help
        printCommand(args[0]);
    }

    private void printCommand(String name) throws CommandException {
        printCommand(name, handler.lookup(name));
    }

    private void printCommands() {
        for (CommandHandler.Category category : handler.getCategories()) {
            printCategory(category.name, category.description);
            for (Entry<String, Command> entry : category.category.commandsMap().entrySet())
                printCommand(entry.getKey(), entry.getValue());
        }
        printCategory("Base", null);
        for (Entry<String, Command> entry : handler.getBaseCategory().commandsMap().entrySet())
            printCommand(entry.getKey(), entry.getValue());

    }
}
