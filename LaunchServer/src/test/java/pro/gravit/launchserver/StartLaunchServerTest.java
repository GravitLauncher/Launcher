package pro.gravit.launchserver;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pro.gravit.launcher.Launcher;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LaunchServerRuntimeConfig;
import pro.gravit.launchserver.manangers.CertificateManager;
import pro.gravit.launchserver.manangers.LaunchServerGsonManager;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;
import pro.gravit.utils.command.StdCommandHandler;
import pro.gravit.utils.helper.SecurityHelper;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public class StartLaunchServerTest {
    @TempDir
    public static Path modulesDir;
    @TempDir
    public static Path configDir;
    @TempDir
    public static Path dir;
    public static LaunchServer launchServer;

    @BeforeAll
    public static void prepare() throws Throwable {
        LaunchServerModulesManager modulesManager = new LaunchServerModulesManager(modulesDir, configDir, null);
        LaunchServerConfig config = LaunchServerConfig.getDefault(LaunchServer.LaunchServerEnv.TEST);
        Launcher.gsonManager = new LaunchServerGsonManager(modulesManager);
        Launcher.gsonManager.initGson();
        LaunchServerRuntimeConfig runtimeConfig = new LaunchServerRuntimeConfig();
        LaunchServerBuilder builder = new LaunchServerBuilder();
        KeyPair pair = SecurityHelper.genECKeyPair(new SecureRandom());
        ECPublicKey publicKey = (ECPublicKey) pair.getPublic();
        ECPrivateKey privateKey = (ECPrivateKey) pair.getPrivate();
        builder.setDir(dir)
                .setEnv(LaunchServer.LaunchServerEnv.TEST)
                .setConfig(config)
                .setRuntimeConfig(runtimeConfig)
                .setPublicKey(publicKey)
                .setPrivateKey(privateKey)
                .setCertificateManager(new CertificateManager())
                .setLaunchServerConfigManager(new LaunchServer.LaunchServerConfigManager() {
                    @Override
                    public LaunchServerConfig readConfig() {
                        return LaunchServerConfig.getDefault(LaunchServer.LaunchServerEnv.TEST);
                    }

                    @Override
                    public LaunchServerRuntimeConfig readRuntimeConfig() {
                        LaunchServerRuntimeConfig r = new LaunchServerRuntimeConfig();
                        r.reset();
                        return r;
                    }

                    @Override
                    public void writeConfig(LaunchServerConfig config) {

                    }

                    @Override
                    public void writeRuntimeConfig(LaunchServerRuntimeConfig config) {

                    }
                })
                .setModulesManager(modulesManager)
                .setCommandHandler(new StdCommandHandler(false));
        launchServer = builder.build();
    }

    @AfterAll
    public static void complete() throws Throwable {
        launchServer.close();
    }

    @Test
    public void start() {
        launchServer.run();
    }
}
