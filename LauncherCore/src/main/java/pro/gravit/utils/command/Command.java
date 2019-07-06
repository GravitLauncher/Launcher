package pro.gravit.utils.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jline.reader.Candidate;
import pro.gravit.utils.helper.VerifyHelper;

public abstract class Command {


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
        return new ArrayList<>();
    }

    public List<Candidate> subCommandComplete(String word, List<String> commands)
    {
        List<Candidate> candidates = new ArrayList<>();
        for(String s : commands)
        {
            if(word.startsWith(s))
            {
                candidates.add(new Candidate(s));
            }
        }
        return candidates;
    }


    public abstract void invoke(String... args) throws Exception;


    protected final void verifyArgs(String[] args, int min) throws CommandException {
        if (args.length < min)
            throw new CommandException("Command usage: " + getArgsDescription());
    }
}
