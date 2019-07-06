package pro.gravit.utils.command;

import java.util.*;

import org.jline.reader.Candidate;
import pro.gravit.utils.helper.VerifyHelper;

public abstract class Command {
    public Map<String, Command> childCommands = new HashMap<>();

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

    public Candidate buildCandidate(CommandHandler.Category category, String commandName)
    {
        return new Candidate(commandName);
    }

    public List<Candidate> complete(List<String> words, int wordIndex, String word)
    {
        if(wordIndex == 0)
        {
            List<Candidate> candidates = new ArrayList<>();
            childCommands.forEach((k,v) -> {
                if(k.startsWith(word))
                {
                    candidates.add(new Candidate(k));
                }
            });
            return candidates;
        }
        else
        {
            Command cmd = childCommands.get(words.get(0));
            if(cmd == null) return new ArrayList<>();
            return cmd.complete(words.subList(1, words.size()), wordIndex - 1, word);
        }
    }

    public void invokeSubcommands(String... args) throws Exception
    {
        verifyArgs(args, 1);
        Command command = childCommands.get(args[0]);
        if(command == null) throw new CommandException(String.format("Unknown sub command: '%s'", args[0]));
        command.invoke(Arrays.copyOfRange(args, 1, args.length));
    }


    public abstract void invoke(String... args) throws Exception;


    protected final void verifyArgs(String[] args, int min) throws CommandException {
        if (args.length < min)
            throw new CommandException("Command usage: " + getArgsDescription());
    }
}
