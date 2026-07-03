package pro.gravit.utils.command;

import org.fusesource.jansi.Ansi;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class JLineCommandHandler extends CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(JLineCommandHandler.class);
    private static Terminal terminal;
    private static LineReader reader;

    public JLineCommandHandler() throws IOException {
        super();

        if (terminal == null) {
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .nativeSignals(true)
                    .signalHandler(Terminal.SignalHandler.SIG_IGN)
                    .build();

            Completer completer = new JLineConsoleCompleter();

            DefaultHistory filteredHistory = new DefaultHistory() {
                @Override
                public void add(Instant time, String line) {
                    if (line == null || line.trim().isEmpty()) return;
                    String lowerLine = line.toLowerCase().trim();
                    if (lowerLine.contains("register") || lowerLine.contains("auth"))
                        return;
                    super.add(time, line);
                }
            };

            reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(completer)
                    .history(filteredHistory)
                    .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                    .option(LineReader.Option.AUTO_FRESH_LINE, true)
                    .build();

            KeyMap<Binding> mainKeyMap = reader.getKeyMaps().get(LineReader.MAIN);
            mainKeyMap.bind(new Reference(LineReader.UP_LINE_OR_HISTORY), KeyMap.key(terminal, InfoCmp.Capability.key_up));
            mainKeyMap.bind(new Reference(LineReader.DOWN_LINE_OR_HISTORY), KeyMap.key(terminal, InfoCmp.Capability.key_down));
            mainKeyMap.bind(new Reference(LineReader.UP_LINE_OR_HISTORY), "\033[A");
            mainKeyMap.bind(new Reference(LineReader.DOWN_LINE_OR_HISTORY), "\033[B");

            mainKeyMap.bind(new Reference(LineReader.KILL_LINE), "\003");
            reader.setVariable(LineReader.HISTORY_FILE, IOHelper.WORKING_DIR.resolve(".launcher_history"));
            filteredHistory.attach(reader);
        }
    }

    @Override
    public void bell() {
        terminal.puts(InfoCmp.Capability.bell);
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
            if (reader.getBuffer().length() == 0) {
                logger.info(new Ansi().bold().fgBright(Ansi.Color.YELLOW).a("Exit triggered by Ctrl+C. Cleaning up resources...").reset().toString());
                JVMHelper.RUNTIME.exit(0);
                return null;
            } else {
                return "";
            }

        } catch (EndOfFileException e) {
            try {
                logger.info(new Ansi().bold().fgBright(Ansi.Color.YELLOW).a("Exit triggered by Ctrl+D. Cleaning up resources...").reset().toString());
                terminal.close();
            } catch (IOException ignored) {
            }
            JVMHelper.RUNTIME.exit(0);
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
                if (target == null) return;
                List<String> words = line.words();
                List<Candidate> candidates1 = target.complete(words.subList(1, words.size()), line.wordIndex() - 1, completeWord);
                if (candidates1 != null) candidates.addAll(candidates1);
            }
        }
    }
}
