package pro.gravit.utils.command;

import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class JLineCommandHandler extends CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(JLineCommandHandler.class);
    private Terminal terminal;
    private LineReader reader;
    private final Object readLock = new Object();
    private final Object outputLock = new Object();
    private volatile Thread readingThread;

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
                    if (lowerLine.contains("register") || lowerLine.contains("auth")
                            || lowerLine.contains("config") || lowerLine.contains("token")
                            || lowerLine.contains("password") || lowerLine.contains("secret"))
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
        synchronized (outputLock) {
            if (!isStopRequested()) terminal.puts(InfoCmp.Capability.bell);
        }
    }

    @Override
    public void clear() {
        synchronized (outputLock) {
            if (!isStopRequested()) terminal.puts(InfoCmp.Capability.clear_screen);
        }
    }

    @Override
    public String readLine() {
        synchronized (readLock) {
            readingThread = Thread.currentThread();
            try {
                if (isStopRequested()) return null;
                return reader.readLine();
            } catch (UserInterruptException e) {
                if (isStopRequested()) return null;
                if (reader.getBuffer().length() == 0) {
                    logger.info("Exit triggered by Ctrl+C");
                    return null;
                }
                return "";
            } catch (EndOfFileException e) {
                if (!isStopRequested()) logger.info("Exit triggered by Ctrl+D");
                return null;
            } finally {
                readingThread = null;
            }
        }
    }

    @Override
    public void requestStop() {
        super.requestStop();
        Thread activeReader = readingThread;
        if (activeReader != null && activeReader != Thread.currentThread()) activeReader.interrupt();
    }

    public void closeTerminal() throws IOException {
        requestStop();
        // JLine restores terminal state in readLine's finally block before this lock is released.
        synchronized (readLock) {
            synchronized (outputLock) {
                if (terminal != null) {
                    terminal.close();
                    terminal = null;
                    reader = null;
                }
            }
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
