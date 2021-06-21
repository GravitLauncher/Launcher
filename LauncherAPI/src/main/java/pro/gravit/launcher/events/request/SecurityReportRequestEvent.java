package pro.gravit.launcher.events.request;

import pro.gravit.launcher.events.RequestEvent;

public class SecurityReportRequestEvent extends RequestEvent {
    public final ReportAction action;
    public final String otherAction;

    public SecurityReportRequestEvent(ReportAction action) {
        this.action = action;
        this.otherAction = null;
    }

    public SecurityReportRequestEvent(String otherAction) {
        this.action = ReportAction.OTHER;
        this.otherAction = otherAction;
    }

    public SecurityReportRequestEvent() {
        this.action = ReportAction.NONE;
        this.otherAction = null;
    }

    @Override
    public String getType() {
        return "securityReport";
    }

    public enum ReportAction {
        NONE,
        LOGOUT,
        TOKEN_EXPIRED,
        EXIT,
        CRASH,
        OTHER
    }
}
