package pro.gravit.launcher.guard;

import pro.gravit.launcher.client.ClientLauncherContext;
import pro.gravit.launcher.client.ClientLauncherProcess;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class LauncherNoGuard implements LauncherGuardInterface {
    @Override
    public String getName() {
        return "noGuard";
    }

    @Override
    public void applyGuardParams(ClientLauncherProcess process) {
        //IGNORED
    }
}
