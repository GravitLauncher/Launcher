package pro.gravit.utils.command;

import org.jline.reader.Candidate;
import pro.gravit.utils.helper.VerifyHelper;

import java.util.*;

public abstract class Command {
    /**
     * List of available subcommands
     */
    public final Map<String, Command> childCommands;

    public Command() {
        childCommands = new HashMap<>();
    }

    public Command(Map<String, Command> childCommands) {
        this.childCommands = childCommands;
    }

    protected static String parseUsername(String username) throws CommandException {
        try {
            return VerifyHelper.verifyUsername(username);
        } catch (IllegalArgumentException e) {
            throw new CommandException(e.getMessage());
        }
    }


    protected static UUID parseUUID(String s) throws CommandException {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ignored) {
            throw new CommandException(String.format("Invalid UUID: '%s'", s));
        }
    }

    public abstract String getArgsDescription(); // "<required> [optional]"


    public abstract String getUsageDescription();

    /**
     * Creates a JLine candidate that appears in the list of available options when you press TAB
     *
     * @param category    this command category
     * @param commandName this command name
     * @return JLine Candidate
     */
    public Candidate buildCandidate(CommandHandler.Category category, String commandName) {
        return new Candidate(commandName);
    }

    /**
     * Returns a list of available options for the next word for the current command.
     *
     * @param words     list all user words
     * @param wordIndex current word index
     * @param word      current word
     * @return list of available Candidate
     */
    public List<Candidate> complete(List<String> words, int wordIndex, String word) {
        if (wordIndex == 0) {
            List<Candidate> candidates = new ArrayList<>();
            childCommands.forEach((k, v) -> {
                if (k.startsWith(word)) {
                    candidates.add(new Candidate(k));
                }
            });
            return candidates;
        } else {
            Command cmd = childCommands.get(words.get(0));
            if (cmd == null) return new ArrayList<>();
            return cmd.complete(words.subList(1, words.size()), wordIndex - 1, word);
        }
    }

    /**
     * Transfer control to subcommands
     *
     * @param args command arguments(includes subcommand name)
     * @throws Exception Error executing command
     */
    public void invokeSubcommands(String... args) throws Exception {
        verifyArgs(args, 1);
        Command command = childCommands.get(args[0]);
        if (command == null) throw new CommandException(String.format("Unknown sub command: '%s'", args[0]));
        command.invoke(Arrays.copyOfRange(args, 1, args.length));
    }


    /**
     * Run current command
     *
     * @param args command arguments
     * @throws Exception Error executing command
     */
    public abstract void invoke(String... args) throws Exception;


    protected final void verifyArgs(String[] args, int min) throws CommandException {
        if (args.length < min)
            throw new CommandException("Command usage: " + getArgsDescription());
    }
}
