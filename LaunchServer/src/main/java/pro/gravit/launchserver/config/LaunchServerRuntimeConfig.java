package pro.gravit.launchserver.config;

import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

public class LaunchServerRuntimeConfig {
    public String clientToken;
    public String oemUnlockKey;
    public String registerApiKey;
    public String clientCheckSecret;

    public void verify() {
        if (clientToken == null) LogHelper.error("[RuntimeConfig] clientToken must not be null");
        if (clientCheckSecret == null) { LogHelper.warning("[RuntimeConfig] clientCheckSecret must not be null"); clientCheckSecret = SecurityHelper.randomStringToken(); }
    }

    public void reset() {
        clientToken = SecurityHelper.randomStringToken();
        registerApiKey = SecurityHelper.randomStringToken();
        clientCheckSecret = SecurityHelper.randomStringToken();
    }
}
