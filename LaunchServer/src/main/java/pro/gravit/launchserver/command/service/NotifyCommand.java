package pro.gravit.launchserver.command.service;

import pro.gravit.launcher.events.NotificationEvent;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.socket.WebSocketService;

public class NotifyCommand extends Command {
    public NotifyCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[head] [message] (icon)";
    }

    @Override
    public String getUsageDescription() {
        return "send notification to all connected client";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        NotificationEvent event;
        if (args.length < 3) {
            event = new NotificationEvent(args[0], args[1]);
        } else {
            event = new NotificationEvent(args[0], args[1], Enum.valueOf(NotificationEvent.NotificationType.class, args[2]));
        }
        WebSocketService service = server.nettyServerSocketHandler.nettyServer.service;
        service.sendObjectAll(event, WebSocketEvent.class);
    }
}
