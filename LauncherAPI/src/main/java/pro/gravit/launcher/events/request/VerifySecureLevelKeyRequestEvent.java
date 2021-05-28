package pro.gravit.launcher.events.request;

import pro.gravit.launcher.events.RequestEvent;

public class VerifySecureLevelKeyRequestEvent extends RequestEvent {
    public boolean needHardwareInfo;
    public boolean onlyStatisticInfo;
    public String extendedToken;

    public VerifySecureLevelKeyRequestEvent() {
    }

    public VerifySecureLevelKeyRequestEvent(boolean needHardwareInfo) {
        this.needHardwareInfo = needHardwareInfo;
    }

    public VerifySecureLevelKeyRequestEvent(boolean needHardwareInfo, boolean onlyStatisticInfo, String extendedToken) {
        this.needHardwareInfo = needHardwareInfo;
        this.onlyStatisticInfo = onlyStatisticInfo;
        this.extendedToken = extendedToken;
    }

    @Override
    public String getType() {
        return "verifySecureLevelKey";
    }
}
