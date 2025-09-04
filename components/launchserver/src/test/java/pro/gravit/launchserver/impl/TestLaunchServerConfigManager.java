package pro.gravit.launchserver.impl;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LauncherModulesConfig;

import java.io.IOException;

public class TestLaunchServerConfigManager implements LaunchServer.LaunchServerConfigManager {
    public LaunchServerConfig config;
    public LauncherModulesConfig modulesConfig;

    public TestLaunchServerConfigManager() {
        config = LaunchServerConfig.getDefault(LaunchServer.LaunchServerEnv.TEST);
        modulesConfig = new LauncherModulesConfig();
    }

    @Override
    public LaunchServerConfig readConfig() {
        return config;
    }

    @Override
    public void writeConfig(LaunchServerConfig config) {

    }

    @Override
    public LauncherModulesConfig readModulesConfig() throws IOException {
        return modulesConfig;
    }

    @Override
    public void writeModulesConfig(LauncherModulesConfig config) throws IOException {
    }
}
