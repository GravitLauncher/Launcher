package pro.gravit.launchserver.binary;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class JARLauncherBinary extends LauncherBinary {
    public final AtomicLong count;
    public final Path runtimeDir;
    public final Path buildDir;
    public final List<Path> coreLibs;
    public final List<Path> addonLibs;

    public final Map<String, Path> files;

    public JARLauncherBinary(LaunchServer server) throws IOException {
        super(server, resolve(server, ".jar"), "Launcher-%s.jar");
        count = new AtomicLong(0);
        runtimeDir = server.dir.resolve(Launcher.RUNTIME_DIR);
        buildDir = server.dir.resolve("build");
        coreLibs = new ArrayList<>();
        addonLibs = new ArrayList<>();
        files = new HashMap<>();
        if (!Files.isDirectory(buildDir)) {
            Files.deleteIfExists(buildDir);
            Files.createDirectory(buildDir);
        }
    }

    @Override
    public void init() {
        tasks.add(new PrepareBuildTask(server));
        if (!server.config.sign.enabled) tasks.add(new CertificateAutogenTask(server));
        tasks.add(new MainBuildTask(server));
        tasks.add(new AttachJarsTask(server));
        tasks.add(new AdditionalFixesApplyTask(server));
        if (server.config.launcher.compress) tasks.add(new CompressBuildTask(server));
        tasks.add(new SignJarTask(server.config.sign, server));
    }
}
