package pro.gravit.launchserver.binary;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.exe.Launch4JTask;

public final class EXEL4JLauncherBinary extends LauncherBinary {


    public EXEL4JLauncherBinary(LaunchServer server) {
        super(server, LauncherBinary.resolve(server, ".exe"), "Launcher-%s-%d.exe");
    }

    @Override
    public void init() {
        tasks.add(new Launch4JTask(server));
    }
}
