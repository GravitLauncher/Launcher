package ru.gravit.launchserver.console;

import ru.gravit.launcher.events.request.LogEvent;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.utils.command.Command;
import ru.gravit.utils.helper.LogHelper;

public class LogListenerCommand extends Command {
    public class LogListenerRequest implements RequestInterface
    {
        @Override
        public String getType() {
            return "addLogListener";
        }
    }
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
        LogHelper.info("Send log listener request");
        LegacyRequestBridge.service.sendObject(new LogListenerRequest());
        LogHelper.info("Add log handler");
        LegacyRequestBridge.service.registerHandler((result) -> {
            if(result instanceof LogEvent)
            {
                System.out.println(((LogEvent) result).string);
            }
        });
    }
}
