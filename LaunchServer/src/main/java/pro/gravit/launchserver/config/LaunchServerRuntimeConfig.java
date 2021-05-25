package pro.gravit.launchserver.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.utils.helper.SecurityHelper;

public class LaunchServerRuntimeConfig {
    private transient final Logger logger = LogManager.getLogger();
    public String passwordEncryptKey;
    public String runtimeEncryptKey;
    public String oemUnlockKey;
    public String registerApiKey;
    public String clientCheckSecret;

    public void verify() {
        if (passwordEncryptKey == null) logger.error("[RuntimeConfig] passwordEncryptKey must not be null");
        if (clientCheckSecret == null) {
            logger.warn("[RuntimeConfig] clientCheckSecret must not be null");
            clientCheckSecret = SecurityHelper.randomStringToken();
        }
    }

    public void reset() {
        passwordEncryptKey = SecurityHelper.randomStringToken();
        runtimeEncryptKey = SecurityHelper.randomStringAESKey();
        registerApiKey = SecurityHelper.randomStringToken();
        clientCheckSecret = SecurityHelper.randomStringToken();
    }
}
