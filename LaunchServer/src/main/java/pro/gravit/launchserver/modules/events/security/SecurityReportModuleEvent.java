package pro.gravit.launchserver.modules.events.security;

import pro.gravit.launcher.events.request.SecurityReportRequestEvent;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.secure.SecurityReportResponse;

public class SecurityReportModuleEvent extends LauncherModule.Event {
    public final SecurityReportRequestEvent event;
    public final SecurityReportResponse response;
    public final Client client;

    public SecurityReportModuleEvent(SecurityReportRequestEvent event, SecurityReportResponse response, Client client) {
        this.event = event;
        this.response = response;
        this.client = client;
    }
}
