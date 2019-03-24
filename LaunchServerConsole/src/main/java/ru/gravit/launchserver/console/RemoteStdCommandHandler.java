package ru.gravit.launchserver.console;

import ru.gravit.launcher.request.admin.ExecCommandRequest;
import ru.gravit.utils.command.StdCommandHandler;
import ru.gravit.utils.helper.LogHelper;

public class RemoteStdCommandHandler extends StdCommandHandler {
    public RemoteStdCommandHandler(boolean readCommands) {
        super(readCommands);
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
