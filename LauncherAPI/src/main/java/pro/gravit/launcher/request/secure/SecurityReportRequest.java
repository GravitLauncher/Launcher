package pro.gravit.launcher.request.secure;

import pro.gravit.launcher.events.request.SecurityReportRequestEvent;
import pro.gravit.launcher.request.Request;

public final class SecurityReportRequest extends Request<SecurityReportRequestEvent> {
    public final String reportType;
    public final String smallData;
    public final String largeData;
    public final byte[] smallBytes;
    public final byte[] largeBytes;

    public SecurityReportRequest(String reportType, String smallData, String largeData, byte[] smallBytes, byte[] largeBytes) {
        this.reportType = reportType;
        this.smallData = smallData;
        this.largeData = largeData;
        this.smallBytes = smallBytes;
        this.largeBytes = largeBytes;
    }

    public SecurityReportRequest(String reportType, String smallData, String largeData, byte[] smallBytes) {
        this.reportType = reportType;
        this.smallData = smallData;
        this.largeData = largeData;
        this.smallBytes = smallBytes;
        this.largeBytes = null;
    }

    public SecurityReportRequest(String reportType, String smallData, String largeData) {
        this.reportType = reportType;
        this.smallData = smallData;
        this.largeData = largeData;
        this.smallBytes = null;
        this.largeBytes = null;
    }

    public SecurityReportRequest(String reportType, String smallData, byte[] smallBytes) {
        this.reportType = reportType;
        this.smallData = smallData;
        this.largeData = null;
        this.smallBytes = smallBytes;
        this.largeBytes = null;
    }

    public SecurityReportRequest(String reportType, byte[] smallBytes, byte[] largeBytes) {
        this.reportType = reportType;
        this.smallData = null;
        this.largeData = null;
        this.smallBytes = smallBytes;
        this.largeBytes = largeBytes;
    }

    public SecurityReportRequest(String reportType, byte[] smallBytes) {
        this.reportType = reportType;
        this.smallData = null;
        this.largeData = null;
        this.smallBytes = smallBytes;
        this.largeBytes = null;
    }

    public SecurityReportRequest(String reportType, String smallData) {
        this.reportType = reportType;
        this.smallData = smallData;
        this.largeData = null;
        this.smallBytes = null;
        this.largeBytes = null;
    }

    public SecurityReportRequest(String reportType) {
        this.reportType = reportType;
        this.smallData = null;
        this.largeData = null;
        this.smallBytes = null;
        this.largeBytes = null;
    }

    @Override
    public String getType() {
        return "securityReport";
    }
}
