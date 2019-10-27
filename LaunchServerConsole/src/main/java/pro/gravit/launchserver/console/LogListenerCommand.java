package pro.gravit.launchserver.console;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.request.LogEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.WebSocketRequest;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class LogListenerCommand extends Command {
    public static class LogListenerRequest implements WebSocketRequest {
        @LauncherNetworkAPI
        public final LogHelper.OutputTypes outputType;

        public LogListenerRequest(LogHelper.OutputTypes outputType) {
            this.outputType = outputType;
        }

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
        Request.service.sendObject(new LogListenerRequest(LogHelper.JANSI ? LogHelper.OutputTypes.JANSI : LogHelper.OutputTypes.PLAIN));
        LogHelper.info("Add log handler");
        Request.service.registerHandler((result) -> {
            if (result instanceof LogEvent) {
                System.out.println(((LogEvent) result).string);
            }
        });
    }
}
