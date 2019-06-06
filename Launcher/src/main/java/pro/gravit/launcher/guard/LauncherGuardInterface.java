package pro.gravit.launcher.guard;

import java.nio.file.Path;

import pro.gravit.launcher.client.ClientLauncherContext;

public interface LauncherGuardInterface {
    String getName();

    Path getJavaBinPath();

    int getClientJVMBits();

    void init(boolean clientInstance);

    void addCustomParams(ClientLauncherContext context);

    void addCustomEnv(ClientLauncherContext context);

    void setProtectToken(String token);
}
