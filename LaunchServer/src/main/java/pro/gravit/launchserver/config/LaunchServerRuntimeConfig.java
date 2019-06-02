package pro.gravit.launchserver.config;

import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

public class LaunchServerRuntimeConfig {
    public String clientToken;
    public String oemUnlockKey;

    public void verify() {
        if (clientToken == null) LogHelper.error("[RuntimeConfig] clientToken must not be null");
    }

    public void reset() {
        clientToken = SecurityHelper.randomStringToken();
    }
}
