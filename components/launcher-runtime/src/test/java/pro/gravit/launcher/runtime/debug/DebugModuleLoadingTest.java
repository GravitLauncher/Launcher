package pro.gravit.launcher.runtime.debug;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.start.RuntimeModuleManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

public class DebugModuleLoadingTest {
    @Test
    void onlyFileEnabledManagerLoadsJar(@TempDir Path dir) throws Exception {
        var jar = dir.resolve("debug-module.jar");
        var manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Module-Main-Class", DebugModule.class.getName());
        try (var output = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            output.flush();
        }

        Launcher.setConfig(new LauncherConfig("http://localhost", Map.of(), "Test",
                LauncherConfig.LauncherEnvironment.DEBUG,
                new DebugLauncherTrustManager(DebugLauncherTrustManager.TrustDebugMode.TRUST_ALL)));
        try {
            var classpathOnly = new RuntimeModuleManager();
            assertThrows(UnsupportedOperationException.class, () -> classpathOnly.loadModule(jar));
            var debug = new RuntimeModuleManager(true);
            try (var loader = (java.net.URLClassLoader) debug.getModuleClassLoader()) {
                assertInstanceOf(DebugModule.class, debug.loadModule(jar));
                assertArrayEquals(new java.net.URL[]{jar.toUri().toURL()}, loader.getURLs());
            }
        } finally {
            Launcher.setConfig(null);
        }
    }

    public static class DebugModule extends LauncherModule {
        @Override
        public void init(LauncherInitContext context) {
        }
    }
}
