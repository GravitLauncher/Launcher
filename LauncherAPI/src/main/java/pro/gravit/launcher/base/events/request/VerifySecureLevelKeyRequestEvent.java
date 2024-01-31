package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.base.events.ExtendedTokenRequestEvent;
import pro.gravit.launcher.base.events.RequestEvent;

public class VerifySecureLevelKeyRequestEvent extends RequestEvent implements ExtendedTokenRequestEvent {
    public boolean needHardwareInfo;
    public boolean onlyStatisticInfo;
    public String extendedToken;
    public long expire;

    public VerifySecureLevelKeyRequestEvent() {
    }

    public VerifySecureLevelKeyRequestEvent(boolean needHardwareInfo) {
        this.needHardwareInfo = needHardwareInfo;
    }

    public VerifySecureLevelKeyRequestEvent(boolean needHardwareInfo, boolean onlyStatisticInfo, String extendedToken, long expire) {
        this.needHardwareInfo = needHardwareInfo;
        this.onlyStatisticInfo = onlyStatisticInfo;
        this.extendedToken = extendedToken;
        this.expire = expire;
    }

    @Override
    public String getType() {
        return "verifySecureLevelKey";
    }

    @Override
    public String getExtendedTokenName() {
        return "publicKey";
    }

    @Override
    public String getExtendedToken() {
        return extendedToken;
    }

    @Override
    public long getExtendedTokenExpire() {
        return expire;
    }
}
