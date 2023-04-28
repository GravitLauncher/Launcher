package pro.gravit.launchserver.impl;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LaunchServerRuntimeConfig;

public class TestLaunchServerConfigManager implements LaunchServer.LaunchServerConfigManager {
    public LaunchServerConfig config;
    public LaunchServerRuntimeConfig runtimeConfig;

    public TestLaunchServerConfigManager() {
        config = LaunchServerConfig.getDefault(LaunchServer.LaunchServerEnv.TEST);
        runtimeConfig = new LaunchServerRuntimeConfig();
        runtimeConfig.reset();
    }

    @Override
    public LaunchServerConfig readConfig() {
        return config;
    }

    @Override
    public LaunchServerRuntimeConfig readRuntimeConfig() {
        return runtimeConfig;
    }

    @Override
    public void writeConfig(LaunchServerConfig config) {

    }

    @Override
    public void writeRuntimeConfig(LaunchServerRuntimeConfig config) {

    }
}
