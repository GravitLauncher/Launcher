package pro.gravit.launcher.client.params;

import pro.gravit.launcher.client.ClientLauncherProcess;

import java.io.IOException;

public interface ParamsWriter {
    void write(ClientLauncherProcess.ClientParams params) throws IOException;
    ClientLauncherProcess.ClientParams read() throws IOException;
}
