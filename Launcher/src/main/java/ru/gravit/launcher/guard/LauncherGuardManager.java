package ru.gravit.launcher.guard;

import ru.gravit.launcher.client.ClientLauncher;

import java.nio.file.Path;

public class LauncherGuardManager {
    public static LauncherGuardInterface guard;
    public static void initGuard(boolean clientInstance)
    {
        if(ClientLauncher.isUsingWrapper())
        {
            guard = new LauncherWrapperGuard();
        }
        else if(ClientLauncher.isDownloadJava())
        {
            guard = new LauncherJavaGuard();
        }
        else guard = new LauncherNoGuard();
        guard.init(clientInstance);
    }
    public static Path getGuardJavaBinPath()
    {
        return guard.getJavaBinPath();
    }
}
