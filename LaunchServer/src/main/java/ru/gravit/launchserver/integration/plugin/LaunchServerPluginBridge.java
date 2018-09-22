package ru.gravit.launchserver.integration.plugin;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.launchserver.LaunchServer;

public final class LaunchServerPluginBridge implements Runnable, AutoCloseable {
    /**
     * Permission.
     */
    public static final String perm = "launchserver.corecmdcall";
    /**
     * Err text.
     */
    public static final String nonInitText = "Лаунчсервер не был полностью загружен";

    static {
        //SecurityHelper.verifyCertificates(LaunchServer.class);
        JVMHelper.verifySystemProperties(LaunchServer.class, false);
    }

    private final LaunchServer server;

    public LaunchServerPluginBridge(Path dir) throws Throwable {
        LogHelper.addOutput(dir.resolve("LaunchServer.log"));
        LogHelper.printVersion("LaunchServer");

        // Create new LaunchServer
        Instant start = Instant.now();
        try {
            server = new LaunchServer(dir, true);
        } catch (Throwable exc) {
            LogHelper.error(exc);
            throw exc;
        }
        Instant end = Instant.now();
        LogHelper.debug("LaunchServer started in %dms", Duration.between(start, end).toMillis());
    }

    @Override
    public void close() {
        server.close();
    }

    public void eval(String... command) {
        server.commandHandler.eval(command, false);
    }

    @Override
    public void run() {
        server.run();
    }
}
