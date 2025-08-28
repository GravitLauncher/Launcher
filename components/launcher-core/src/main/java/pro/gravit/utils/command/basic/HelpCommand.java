package pro.gravit.utils.command.basic;

import org.fusesource.jansi.Ansi;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.helper.LogHelper;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.function.Supplier;

public final class HelpCommand extends Command {
    private final CommandHandler handler;

    public HelpCommand(CommandHandler handler) {
        this.handler = handler;
    }

    public static void printCommand(String name, Command command) {
        String args = command.getArgsDescription();
        Supplier<String> plaintext = () -> String.format("%s %s - %s", name, args == null ? "[nothing]" : args, command.getUsageDescription());
        Supplier<String> jansitext = () -> {
            Ansi ansi = new Ansi();
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
        };
        LogHelper.logJAnsi(LogHelper.Level.INFO, plaintext, jansitext, true);
    }

    public static void printSubCommandsHelp(String base, Command command) {
        command.childCommands.forEach((k, v) -> printCommand(base.concat(" ").concat(k), v));
    }

    public static void printSubCommandsHelp(String name, String[] args, Command command) throws CommandException {
        if (args.length == 0) {
            printSubCommandsHelp(name, command);
        } else {
            Command child = command.childCommands.get(args[0]);
            if (child == null) throw new CommandException(String.format("Unknown sub command: '%s'", args[0]));
            printSubCommandsHelp(name.concat(" ").concat(args[0]), Arrays.copyOfRange(args, 1, args.length), child);
        }
    }

    private static void printCategory(String name, String description) {
        if (description != null) LogHelper.info("Category: %s - %s", name, description);
        else LogHelper.info("Category: %s", name);
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
        if (args.length == 1)
            printCommand(args[0]);
        printSubCommandsHelp(args[0], Arrays.copyOfRange(args, 1, args.length), handler.lookup(args[0]));
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
