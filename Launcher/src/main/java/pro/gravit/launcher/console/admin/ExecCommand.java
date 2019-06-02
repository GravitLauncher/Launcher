package pro.gravit.launcher.console.admin;

import pro.gravit.utils.command.Command;
import pro.gravit.launcher.events.request.ExecCommandRequestEvent;
import pro.gravit.launcher.request.admin.ExecCommandRequest;
import pro.gravit.utils.helper.LogHelper;

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
        ExecCommandRequestEvent request = new ExecCommandRequest(String.join(" ", args)).request();
        if (!request.success) LogHelper.error("Error executing command");
    }
}
