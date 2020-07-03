package pro.gravit.launcher.events.request;

import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.request.management.PingServerReportRequest;

import java.util.Map;

public class PingServerRequestEvent extends RequestEvent {
    public Map<String, PingServerReportRequest.PingServerReport> serverMap;

    public PingServerRequestEvent() {
    }

    public PingServerRequestEvent(Map<String, PingServerReportRequest.PingServerReport> serverMap) {
        this.serverMap = serverMap;
    }

    @Override
    public String getType() {
        return "pingServer";
    }
}
