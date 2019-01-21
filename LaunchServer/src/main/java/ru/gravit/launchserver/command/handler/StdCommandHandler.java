package ru.gravit.launchserver.command.handler;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;

import java.io.BufferedReader;
import java.io.IOException;

public final class StdCommandHandler extends CommandHandler {
    private final BufferedReader reader;

    public StdCommandHandler(LaunchServer server, boolean readCommands) {
        super(server);
        reader = readCommands ? IOHelper.newReader(System.in) : null;
    }

    @Override
    public void bell() {
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("clear terminal");
    }

    @Override
    public String readLine() throws IOException {
        return reader == null ? null : reader.readLine();
    }
}
