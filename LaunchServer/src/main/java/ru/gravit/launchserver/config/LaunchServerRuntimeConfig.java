package ru.gravit.launchserver.config;

import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

public class LaunchServerRuntimeConfig {
    public String clientToken;
    public void verify()
    {
        if(clientToken == null) LogHelper.error("[RuntimeConfig] clientToken must not be null");
    }
    public void reset()
    {
        clientToken = SecurityHelper.randomStringToken();
    }
}
