package ru.gravit.launchserver.command.handler;

import java.io.IOException;

import jline.console.ConsoleReader;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.LogHelper.Output;
import ru.gravit.launchserver.LaunchServer;

public final class JLineCommandHandler extends CommandHandler {
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

    public JLineCommandHandler(LaunchServer server) throws IOException {
        super(server);

        // Set reader
        reader = new ConsoleReader();
        reader.setExpandEvents(false);

        // Replace writer
        LogHelper.removeStdOutput();
        LogHelper.addOutput(new JLineOutput());
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
