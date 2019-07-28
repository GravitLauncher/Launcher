package pro.gravit.utils.command;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

public class JLineCommandHandler extends CommandHandler {
    /*private final class JLineOutput implements Output {
        @Override
        public void println(String message) {
            try {
                reader.println(ConsoleReader.RESET_LINE + message);
                reader.drawLine();
                reader.flush();
            } catch (IOException ignored) {
                // Ignored
            }
        }
    }*/

    private final Terminal terminal;
    private final TerminalBuilder terminalBuilder;
    private final Completer completer;
    private final LineReader reader;

    public class JLineConsoleCompleter implements Completer {
        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            String completeWord = line.word();
            if (line.wordIndex() == 0)
            {
                walk((category, name, command) -> {
                    if (name.startsWith(completeWord)) {
                        candidates.add(command.buildCandidate(category, name));
                    }
                });
            }
            else
            {
                Command target = findCommand(line.words().get(0));
                List<String> words = line.words();
                List<Candidate> candidates1 = target.complete(words.subList(1, words.size()), line.wordIndex() - 1, completeWord);
                candidates.addAll(candidates1);
            }
        }
    }

    public JLineCommandHandler() throws IOException {
        super();
        terminalBuilder = TerminalBuilder.builder();
        terminal = terminalBuilder.build();
        completer = new JLineConsoleCompleter();
        reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .build();

        // Set reader
        //reader = new ConsoleReader();
        //reader.setExpandEvents(false);

        // Replace writer
        //LogHelper.removeStdOutput();
        //LogHelper.addOutput(new JLineOutput(), LogHelper.OutputTypes.JANSI);
    }

    @Override
    public void bell() throws IOException {
        terminal.puts(InfoCmp.Capability.bell);
        //reader.beep();
    }

    @Override
    public void clear() throws IOException {
        terminal.puts(InfoCmp.Capability.clear_screen);
    }

    @Override
    public String readLine() throws IOException {
        try {
            return reader.readLine();
        } catch (UserInterruptException e) {
            System.exit(0);
            return null;
        }
    }
}
