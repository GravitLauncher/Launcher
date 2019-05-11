package ru.gravit.utils.command;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.LogHelper.Output;

import java.io.IOException;

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
    private final LineReader reader;

    public JLineCommandHandler() throws IOException {
        super();
        terminalBuilder = TerminalBuilder.builder();
        terminal = terminalBuilder.build();
        reader = LineReaderBuilder.builder()
                .terminal(terminal)
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

        //reader.beep();
    }

    @Override
    public void clear() throws IOException {
        terminal.puts(InfoCmp.Capability.clear_screen);
    }

    @Override
    public String readLine() throws IOException {
        return reader.readLine();
    }
}
