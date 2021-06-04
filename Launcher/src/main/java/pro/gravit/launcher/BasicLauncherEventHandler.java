package pro.gravit.launcher;

import pro.gravit.launcher.events.ExtendedTokenRequestEvent;
import pro.gravit.launcher.events.request.SecurityReportRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launcher.request.websockets.ClientWebSocketService;

public class BasicLauncherEventHandler implements ClientWebSocketService.EventHandler {

    @Override
    public <T extends WebSocketEvent> boolean eventHandle(T event) {
        if (event instanceof SecurityReportRequestEvent) {
            SecurityReportRequestEvent event1 = (SecurityReportRequestEvent) event;
            if (event1.action == SecurityReportRequestEvent.ReportAction.CRASH) {
                LauncherEngine.exitLauncher(80);
            }
        }
        if (event instanceof ExtendedTokenRequestEvent) {
            ExtendedTokenRequestEvent event1 = (ExtendedTokenRequestEvent) event;
            String token = event1.getExtendedToken();
            if (token != null) {
                Request.addExtendedToken(event1.getExtendedTokenName(), token);
            }
        }
        return false;
    }
}
