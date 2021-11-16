package pro.gravit.launcher;

import pro.gravit.launcher.api.DialogService;
import pro.gravit.launcher.events.ExtendedTokenRequestEvent;
import pro.gravit.launcher.events.NotificationEvent;
import pro.gravit.launcher.events.request.SecurityReportRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestService;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launcher.request.websockets.ClientWebSocketService;
import pro.gravit.utils.helper.LogHelper;

public class BasicLauncherEventHandler implements RequestService.EventHandler {

    @Override
    public <T extends WebSocketEvent> boolean eventHandle(T event) {
        if (event instanceof SecurityReportRequestEvent) {
            SecurityReportRequestEvent event1 = (SecurityReportRequestEvent) event;
            if (event1.action == SecurityReportRequestEvent.ReportAction.CRASH) {
                LauncherEngine.exitLauncher(80);
            } else if (event1.action == SecurityReportRequestEvent.ReportAction.TOKEN_EXPIRED) {
                try {
                    Request.restore();
                } catch (Exception e) {
                    LogHelper.error(e);
                }
            }
        } else if (event instanceof ExtendedTokenRequestEvent) {
            ExtendedTokenRequestEvent event1 = (ExtendedTokenRequestEvent) event;
            String token = event1.getExtendedToken();
            if (token != null) {
                Request.addExtendedToken(event1.getExtendedTokenName(), token);
            }
        } else if (event instanceof NotificationEvent) {
            NotificationEvent n = (NotificationEvent) event;
            if (DialogService.isNotificationsAvailable()) {
                DialogService.createNotification(n.icon, n.head, n.message);
            }
        }
        return false;
    }
}
