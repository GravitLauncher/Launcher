package com.gravitlauncher.launchserver.cabinetstarter;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.modules.events.PreConfigPhase;
import pro.gravit.launcher.core.LauncherTrustManager;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.LaunchServerBuilder;
import pro.gravit.launchserver.LaunchServerStarter;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LauncherModulesConfig;
import pro.gravit.launchserver.manangers.CertificateManager;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.command.JLineCommandHandler;
import pro.gravit.utils.command.StdCommandHandler;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import static pro.gravit.launchserver.LaunchServerStarter.*;

@Configuration
@Import(CabinetStarterRunner.class)
public class CabinetStarterConfiguration {
    private Logger logger = LoggerFactory.getLogger(CabinetStarterConfiguration.class);

    public CabinetStarterConfiguration() {
        logger.info("Hello World from CabinetStarter");
    }


    @Bean
    public LaunchServer initializeLaunchServer() throws Exception {
        LogHelper.printVersion("LaunchServer");
        LogHelper.printLicense("LaunchServer");
        Path dir = IOHelper.WORKING_DIR;
        Path configFile;
        try {
            Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            Security.addProvider(new BouncyCastleProvider());
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            throw new RuntimeException("Library BouncyCastle not found! Is directory 'libraries' empty?", ex);
        }
        LaunchServer.LaunchServerDirectories directories = new LaunchServer.LaunchServerDirectories();
        directories.dir = dir;
        directories.collect();
        CertificateManager certificateManager = new CertificateManager();
        try {
            certificateManager.readTrustStore(directories.trustStore);
        } catch (CertificateException e) {
            throw new IOException(e);
        }
        {
            LauncherTrustManager.CheckClassResult result = certificateManager.checkClass(LaunchServer.class);
            if (result.type == LauncherTrustManager.CheckClassResultType.SUCCESS) {
                logger.info("LaunchServer signed by {}", result.endCertificate.getSubjectX500Principal().getName());
            } else if (result.type == LauncherTrustManager.CheckClassResultType.NOT_SIGNED) {
                // None
            } else {
                if (result.exception != null) {
                    logger.error("Exception", result.exception);
                }
                logger.warn("LaunchServer signed incorrectly. Status: {}", result.type.name());
            }
        }

        LaunchServerConfig config;
        LaunchServer.LaunchServerEnv env = LaunchServer.LaunchServerEnv.PRODUCTION;
        LaunchServerModulesManager modulesManager = new LaunchServerModulesManager(directories.modules, dir.resolve("config"), certificateManager.trustManager);
        //modulesManager.autoload();
        Path modulesConfigFile = dir.resolve("modules.json");
        LauncherModulesConfig modulesConfig = loadLaunchServerModules(modulesManager, modulesConfigFile, directories.modules);
        modulesManager.initModules(null);
        registerAll();
        initGson(modulesManager);
        printExperimentalBranch();
        if (IOHelper.exists(dir.resolve("LaunchServer.conf"))) {
            configFile = dir.resolve("LaunchServer.conf");
        } else {
            configFile = dir.resolve("LaunchServer.json");
        }
        CommandHandler localCommandHandler;
        try {
            Class.forName("org.jline.terminal.Terminal");

            // JLine2 available
            localCommandHandler = new JLineCommandHandler();
            logger.info("JLine2 terminal enabled");
        } catch (ClassNotFoundException ignored) {
            localCommandHandler = new StdCommandHandler(true);
            logger.warn("JLine2 isn't in classpath, using std");
        }
        modulesManager.invokeEvent(new PreConfigPhase());
        generateConfigIfNotExists(configFile, localCommandHandler, env);
        logger.info("Reading LaunchServer config file");
        try (BufferedReader reader = IOHelper.newReader(configFile)) {
            config = Launcher.gsonManager.gson.fromJson(reader, LaunchServerConfig.class);
        }

        LaunchServer.LaunchServerConfigManager launchServerConfigManager = new LaunchServerStarter.BasicLaunchServerConfigManager(configFile, modulesConfigFile);
        LaunchServer server = new LaunchServerBuilder()
                .setDirectories(directories)
                .setEnv(env)
                .setCommandHandler(localCommandHandler)
                .setConfig(config)
                .setModulesManager(modulesManager)
                .setLaunchServerConfigManager(launchServerConfigManager)
                .setCertificateManager(certificateManager)
                .setModulesConfig(modulesConfig)
                .build();
        return server;
    }
}
