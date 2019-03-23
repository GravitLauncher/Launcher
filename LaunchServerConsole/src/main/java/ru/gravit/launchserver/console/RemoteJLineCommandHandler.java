package ru.gravit.launchserver.console;

import ru.gravit.launcher.request.admin.ExecCommandRequest;
import ru.gravit.utils.command.JLineCommandHandler;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;

public class RemoteJLineCommandHandler extends JLineCommandHandler {
    public RemoteJLineCommandHandler() throws IOException {
    }
    @Override
    public void eval(String line, boolean bell)
    {
        if(line.equals("exit")) System.exit(0);
        ExecCommandRequest request = new ExecCommandRequest(System.out::println, line);
        try {
            request.request();
        } catch (Exception e) {
            LogHelper.error(e);
        }
    }
}
