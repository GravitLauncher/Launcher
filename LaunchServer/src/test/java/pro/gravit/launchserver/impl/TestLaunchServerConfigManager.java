package pro.gravit.launchserver.impl;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LaunchServerRuntimeConfig;

import java.io.IOException;

public class TestLaunchServerConfigManager implements LaunchServer.LaunchServerConfigManager {
    public LaunchServerConfig config;
    public LaunchServerRuntimeConfig runtimeConfig;

    public TestLaunchServerConfigManager() {
        config = LaunchServerConfig.getDefault(LaunchServer.LaunchServerEnv.TEST);
        runtimeConfig = new LaunchServerRuntimeConfig();
        runtimeConfig.reset();
    }

    @Override
    public LaunchServerConfig readConfig() throws IOException {
        return config;
    }

    @Override
    public LaunchServerRuntimeConfig readRuntimeConfig() throws IOException {
        return runtimeConfig;
    }

    @Override
    public void writeConfig(LaunchServerConfig config) throws IOException {

    }

    @Override
    public void writeRuntimeConfig(LaunchServerRuntimeConfig config) throws IOException {

    }
}
