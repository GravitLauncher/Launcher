package pro.gravit.utils.command;

import org.jline.reader.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.util.List;

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
    private final LineReader reader;

    public JLineCommandHandler() throws IOException {
        super();
        TerminalBuilder terminalBuilder = TerminalBuilder.builder();
        terminal = terminalBuilder.build();
        Completer completer = new JLineConsoleCompleter();
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
    public void bell() {
        terminal.puts(InfoCmp.Capability.bell);
        //reader.beep();
    }

    @Override
    public void clear() {
        terminal.puts(InfoCmp.Capability.clear_screen);
    }

    @Override
    public String readLine() {
        try {
            return reader.readLine();
        } catch (UserInterruptException e) {
            System.exit(0);
            return null;
        }
    }

    public class JLineConsoleCompleter implements Completer {
        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            String completeWord = line.word();
            if (line.wordIndex() == 0) {
                walk((category, name, command) -> {
                    if (name.startsWith(completeWord)) {
                        candidates.add(command.buildCandidate(category, name));
                    }
                });
            } else {
                Command target = findCommand(line.words().get(0));
                if(target == null) {
                    return;
                }
                List<String> words = line.words();
                List<Candidate> candidates1 = target.complete(words.subList(1, words.size()), line.wordIndex() - 1, completeWord);
                candidates.addAll(candidates1);
            }
        }
    }
}
