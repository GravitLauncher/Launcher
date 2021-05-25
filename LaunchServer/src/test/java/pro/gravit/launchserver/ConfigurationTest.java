package pro.gravit.launchserver;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.handler.MemoryAuthHandler;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.launchserver.auth.texture.NullTextureProvider;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LaunchServerRuntimeConfig;
import pro.gravit.launchserver.impl.TestLaunchServerConfigManager;
import pro.gravit.launchserver.manangers.CertificateManager;
import pro.gravit.launchserver.manangers.LaunchServerGsonManager;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;
import pro.gravit.utils.command.StdCommandHandler;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.security.Security;

public class ConfigurationTest {
    @TempDir
    public static Path modulesDir;
    @TempDir
    public static Path configDir;
    @TempDir
    public static Path dir;
    public static LaunchServer launchServer;
    public static TestLaunchServerConfigManager launchServerConfigManager;

    @BeforeAll
    public static void prepare() throws Throwable {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
        LaunchServerModulesManager modulesManager = new LaunchServerModulesManager(modulesDir, configDir, null);
        LaunchServerConfig config = LaunchServerConfig.getDefault(LaunchServer.LaunchServerEnv.TEST);
        Launcher.gsonManager = new LaunchServerGsonManager(modulesManager);
        Launcher.gsonManager.initGson();
        LaunchServerRuntimeConfig runtimeConfig = new LaunchServerRuntimeConfig();
        LaunchServerBuilder builder = new LaunchServerBuilder();
        launchServerConfigManager = new TestLaunchServerConfigManager();
        builder.setDir(dir)
                .setEnv(LaunchServer.LaunchServerEnv.TEST)
                .setConfig(config)
                .setRuntimeConfig(runtimeConfig)
                .setCertificateManager(new CertificateManager())
                .setLaunchServerConfigManager(launchServerConfigManager)
                .setModulesManager(modulesManager)
                .setCommandHandler(new StdCommandHandler(false));
        launchServer = builder.build();
    }

    @Test
    public void reloadTest() throws Exception {
        AuthProvider provider = new AuthProvider() {
            @Override
            public AuthProviderResult auth(String login, AuthRequest.AuthPasswordInterface password, String ip) throws Exception {
                if (!(password instanceof AuthPlainPassword)) throw new UnsupportedOperationException();
                if (login.equals("test") && ((AuthPlainPassword) password).password.equals("test")) {
                    return new AuthProviderResult(login, SecurityHelper.randomStringToken(), new ClientPermissions());
                }
                throw new AuthException("Incorrect password");
            }

            @Override
            public void close() throws IOException {

            }
        };
        AuthProviderPair pair = new AuthProviderPair(provider, new MemoryAuthHandler(), new NullTextureProvider());
        launchServerConfigManager.config.auth.put("std", pair);
        launchServer.reload(LaunchServer.ReloadType.FULL);
    }
}
