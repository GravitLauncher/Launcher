package pro.gravit.launcher.server.setup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.events.request.GetPublicKeyRequestEvent;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileVersions;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.auth.GetPublicKeyRequest;
import pro.gravit.launcher.base.request.websockets.StdWebSocketService;
import pro.gravit.launcher.server.ServerWrapper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

public class ServerWrapperSetup {

    private static final Logger logger =
            LoggerFactory.getLogger(ServerWrapperSetup.class);

    public ServerWrapperCommands commands;
    public URLClassLoader urlClassLoader;

    public ServerWrapperSetup() throws IOException {
        commands = new ServerWrapperCommands();
    }

    public void run() throws Exception {
        ServerWrapper wrapper = ServerWrapper.wrapper;
        String jarName = System.getenv("SERVERWRAPPER_JAR_NAME");
        if(jarName == null) {
            System.out.println("Print server jar filename:");
            jarName = commands.commandHandler.readLine();
        }
        Path jarPath = Paths.get(jarName);
        String mainClassName;
        String agentClassName;
        try (JarFile file = new JarFile(jarPath.toFile())) {
            URL jarURL = jarPath.toUri().toURL();
            urlClassLoader = new URLClassLoader(new URL[]{jarURL});
            logger.info("Check server jar MainClass");
            mainClassName = file.getManifest().getMainAttributes().getValue("Main-Class");
            agentClassName = file.getManifest().getMainAttributes().getValue("Premain-Class");
            if (mainClassName == null) {
                logger.error("Main-Class not found in MANIFEST");
                return;
            }
            try {
                Class.forName(mainClassName, false, urlClassLoader);
            } catch (ClassNotFoundException e) {
                logger.error("", e);
                return;
            }
        }
        logger.info("Found MainClass {}", mainClassName);
        if (agentClassName != null) {
            logger.info("Found PremainClass {}", agentClassName);
        }
        if(wrapper.config.serverName == null || wrapper.config.serverName.isEmpty()) {
            System.out.println("Print your server name:");
            wrapper.config.serverName = commands.commandHandler.readLine();
        }
        wrapper.config.mainclass = mainClassName;
        boolean altMode = false;
        for (int i = 0; i < 10; ++i) {
            if(!Request.isAvailable() || Request.getRequestService().isClosed()) {
                if(wrapper.config.address == null || wrapper.config.address.isEmpty()) {
                    System.out.println("Print launchserver websocket host( ws://host:port/api ):");
                    wrapper.config.address = commands.commandHandler.readLine();
                }
                StdWebSocketService service;
                try {
                    service = StdWebSocketService.initWebSockets(wrapper.config.address).get();
                } catch (Throwable e) {
                    logger.error("", e);
                    continue;
                }
                Request.setRequestService(service);
            }
            if(wrapper.config.extendedTokens == null || wrapper.config.extendedTokens.get("checkServer") == null) {
                System.out.println("Print server token:");
                String checkServerToken = commands.commandHandler.readLine();
                wrapper.config.extendedTokens.put("checkServer", new Request.ExtendedToken(checkServerToken, 0));
            }
            wrapper.updateLauncherConfig();
            try {
                wrapper.restore();
                wrapper.getProfiles();
                GetPublicKeyRequestEvent publicKeyRequestEvent = new GetPublicKeyRequest().request();
                wrapper.config.encodedServerRsaPublicKey = publicKeyRequestEvent.rsaPublicKey;
                wrapper.config.encodedServerEcPublicKey = publicKeyRequestEvent.ecdsaPublicKey;
                break;
            } catch (Throwable e) {
                logger.error("", e);
                if(Request.isAvailable() && Request.getRequestService() instanceof AutoCloseable) {
                    ((AutoCloseable) Request.getRequestService()).close();
                }
            }
        }
        if(wrapper.profile != null && wrapper.profile.getVersion().compareTo(ClientProfileVersions.MINECRAFT_1_18) >= 0) {
            logger.info("Switch to alternative start mode (1.18)");
            if(!wrapper.config.classpath.contains(jarName)) {
                wrapper.config.classpath.add(jarName);
            }
            wrapper.config.classLoaderConfig = ClientProfile.ClassLoaderConfig.LAUNCHER;
            altMode = true;
        }
        wrapper.saveConfig();
        logger.info("Generate start script");
        Path startScript;
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) startScript = Paths.get("start.bat");
        else startScript = Paths.get("start.sh");
        if (Files.exists(startScript)) {
            logger.warn("start script found. Move to start.bak");
            Path startScriptBak = Paths.get("start.bak");
            IOHelper.move(startScript, startScriptBak);
        }
        try (Writer writer = IOHelper.newWriter(startScript)) {
            if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
                writer.append("#!/bin/bash\n\n");
            }
            writer.append("\"");
            writer.append(IOHelper.resolveJavaBin(Paths.get(System.getProperty("java.home")), true).toAbsolutePath().toString());
            writer.append("\" ");
            if (mainClassName.contains("bungee")) {
                logger.info("Found BungeeCord mainclass. Modules dir change to modules_srv");
                writer.append(JVMHelper.jvmProperty("serverwrapper.modulesDir", "modules_srv"));
                writer.append(" ");
            }
            if (agentClassName != null) {
                writer.append("-javaagent:ServerWrapper.jar ");
                writer.append("-Dserverwrapper.agentproxy=".concat(agentClassName));
                writer.append(" ");
            }
            //More args
            writer.append("-cp ");
            String pathServerWrapper = IOHelper.getCodeSource(ServerWrapper.class).getFileName().toString();
            writer.append(pathServerWrapper);
            if(!altMode) {
                if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
                    writer.append(";");
                } else writer.append(":");
                writer.append(jarName);
            }
            writer.append(" ");
            writer.append(ServerWrapper.class.getName());
            writer.append("\n");
        }
        if(JVMHelper.OS_TYPE != JVMHelper.OS.MUSTDIE) {
            if(!startScript.toFile().setExecutable(true)) {
                logger.error("Failed to set executable {}", startScript);
            }
        }
    }
}