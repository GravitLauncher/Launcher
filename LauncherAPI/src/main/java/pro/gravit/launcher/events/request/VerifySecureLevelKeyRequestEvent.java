package pro.gravit.launcher.events.request;

import pro.gravit.launcher.events.RequestEvent;

public class VerifySecureLevelKeyRequestEvent extends RequestEvent {
    public boolean needHardwareInfo;
    public boolean onlyStatisticInfo;

    public VerifySecureLevelKeyRequestEvent() {
    }

    public VerifySecureLevelKeyRequestEvent(boolean needHardwareInfo) {
        this.needHardwareInfo = needHardwareInfo;
    }

    @Override
    public String getType() {
        return "verifySecureLevelKey";
    }
}
