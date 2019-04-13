package ru.gravit.launchserver.console;

import ru.gravit.launcher.events.request.ExecCommandRequestEvent;
import ru.gravit.launcher.request.admin.ExecCommandRequest;
import ru.gravit.utils.command.Command;
import ru.gravit.utils.helper.LogHelper;

public class ExecCommand extends Command {
    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return null;
    }

    @Override
    public void invoke(String... args) throws Exception {
        ExecCommandRequestEvent request = new ExecCommandRequest(String.join(" ")).request();
        if(!request.success) LogHelper.error("Error executing command");
    }
}
