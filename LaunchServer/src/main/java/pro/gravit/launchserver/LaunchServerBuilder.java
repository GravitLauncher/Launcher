package pro.gravit.launchserver;

import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LaunchServerRuntimeConfig;
import pro.gravit.launchserver.manangers.CertificateManager;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;
import pro.gravit.utils.command.CommandHandler;

import java.nio.file.Path;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public class LaunchServerBuilder {
    private LaunchServerConfig config;
    private LaunchServerRuntimeConfig runtimeConfig;
    private CommandHandler commandHandler;
    private LaunchServer.LaunchServerEnv env;
    private LaunchServerModulesManager modulesManager;
    private LaunchServer.LaunchServerDirectories directories = new LaunchServer.LaunchServerDirectories();
    private ECPublicKey publicKey;
    private ECPrivateKey privateKey;
    private CertificateManager certificateManager;
    private LaunchServer.LaunchServerConfigManager launchServerConfigManager;

    public LaunchServerBuilder setConfig(LaunchServerConfig config) {
        this.config = config;
        return this;
    }

    public LaunchServerBuilder setEnv(LaunchServer.LaunchServerEnv env) {
        this.env = env;
        return this;
    }

    public LaunchServerBuilder setModulesManager(LaunchServerModulesManager modulesManager) {
        this.modulesManager = modulesManager;
        return this;
    }

    public LaunchServerBuilder setRuntimeConfig(LaunchServerRuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
        return this;
    }

    public LaunchServerBuilder setCommandHandler(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
        return this;
    }

    public LaunchServerBuilder setDirectories(LaunchServer.LaunchServerDirectories directories) {
        this.directories = directories;
        return this;
    }

    public LaunchServerBuilder setDir(Path dir) {
        this.directories.dir = dir;
        return this;
    }

    public LaunchServerBuilder setPublicKey(ECPublicKey publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    public LaunchServerBuilder setPrivateKey(ECPrivateKey privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public LaunchServerBuilder setLaunchServerConfigManager(LaunchServer.LaunchServerConfigManager launchServerConfigManager) {
        this.launchServerConfigManager = launchServerConfigManager;
        return this;
    }

    public LaunchServer build() throws Exception {
        //if(updatesDir == null) updatesDir = dir.resolve("updates");
        //if(profilesDir == null) profilesDir = dir.resolve("profiles");
        directories.collect();
        if (launchServerConfigManager == null) {
            launchServerConfigManager = new LaunchServer.LaunchServerConfigManager() {
                @Override
                public LaunchServerConfig readConfig() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public LaunchServerRuntimeConfig readRuntimeConfig() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void writeConfig(LaunchServerConfig config) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void writeRuntimeConfig(LaunchServerRuntimeConfig config) {
                    throw new UnsupportedOperationException();
                }
            };
        }
        return new LaunchServer(directories, env, config, runtimeConfig, launchServerConfigManager, modulesManager, publicKey, privateKey, commandHandler, certificateManager);
    }

    public LaunchServerBuilder setCertificateManager(CertificateManager certificateManager) {
        this.certificateManager = certificateManager;
        return this;
    }
}
