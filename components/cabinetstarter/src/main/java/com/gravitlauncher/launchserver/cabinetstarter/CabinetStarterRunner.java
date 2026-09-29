package com.gravitlauncher.launchserver.cabinetstarter;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pro.gravit.launchserver.LaunchServer;

@Component
public class CabinetStarterRunner implements CommandLineRunner {
    private LaunchServer server;

    public CabinetStarterRunner(LaunchServer server) {
        this.server = server;
    }

    @Override
    public void run(String... args) throws Exception {
        server.modulesManager.fullInitializedLaunchServer(server);
        server.run();
    }
}
