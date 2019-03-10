package ru.gravit.utils.command;

import ru.gravit.utils.helper.IOHelper;

import java.io.BufferedReader;
import java.io.IOException;

public class StdCommandHandler extends CommandHandler {
    private final BufferedReader reader;

    public StdCommandHandler(boolean readCommands) {
        super();
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
