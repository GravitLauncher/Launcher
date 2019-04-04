package ru.gravit.launchserver.console;

import ru.gravit.utils.command.JLineCommandHandler;

import java.io.IOException;

public class RemoteJLineCommandHandler extends JLineCommandHandler {
    public RemoteJLineCommandHandler() throws IOException {
    }

    @Override
    public void eval(String line, boolean bell) {
        if (line.equals("exit")) System.exit(0);
    }
}
