package pro.gravit.launcher.guard;

import pro.gravit.launcher.client.ClientLauncherContext;

import java.nio.file.Path;

public interface LauncherGuardInterface {
    String getName();

    Path getJavaBinPath();

    int getClientJVMBits();

    void init(boolean clientInstance);

    void addCustomParams(ClientLauncherContext context);

    void addCustomEnv(ClientLauncherContext context);

    void setProtectToken(String token);
}
