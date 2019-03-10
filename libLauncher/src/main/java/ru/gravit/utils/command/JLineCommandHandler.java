package ru.gravit.utils.command;

import jline.console.ConsoleReader;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.LogHelper.Output;

import java.io.IOException;

public class JLineCommandHandler extends CommandHandler {
    private final class JLineOutput implements Output {
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
    }

    private final ConsoleReader reader;

    public JLineCommandHandler() throws IOException {
        super();

        // Set reader
        reader = new ConsoleReader();
        reader.setExpandEvents(false);

        // Replace writer
        LogHelper.removeStdOutput();
        LogHelper.addOutput(new JLineOutput(), LogHelper.OutputTypes.JANSI);
    }

    @Override
    public void bell() throws IOException {
        reader.beep();
    }

    @Override
    public void clear() throws IOException {
        reader.clearScreen();
    }

    @Override
    public String readLine() throws IOException {
        return reader.readLine();
    }
}
