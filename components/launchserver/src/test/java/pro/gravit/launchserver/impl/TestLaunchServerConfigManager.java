package pro.gravit.launchserver.impl;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.config.LaunchServerConfig;

public class TestLaunchServerConfigManager implements LaunchServer.LaunchServerConfigManager {
    public LaunchServerConfig config;

    public TestLaunchServerConfigManager() {
        config = LaunchServerConfig.getDefault(LaunchServer.LaunchServerEnv.TEST);
    }

    @Override
    public LaunchServerConfig readConfig() {
        return config;
    }

    @Override
    public void writeConfig(LaunchServerConfig config) {

    }
}
