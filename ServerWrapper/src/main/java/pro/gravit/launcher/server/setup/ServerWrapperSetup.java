package pro.gravit.launcher.server.setup;

import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.server.ServerWrapper;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.jar.JarFile;

public class ServerWrapperSetup {
    public ServerWrapperCommands commands;
    public PublicURLClassLoader urlClassLoader;

    public ServerWrapperSetup() throws IOException {
        commands = new ServerWrapperCommands();
    }

    public void run() throws Exception {
        ServerWrapper wrapper = ServerWrapper.wrapper;
        System.out.println("Print server jar filename:");
        String jarName = commands.commandHandler.readLine();
        Path jarPath = Paths.get(jarName);
        String mainClassName;
        String agentClassName;
        try (JarFile file = new JarFile(jarPath.toFile())) {
            URL jarURL = jarPath.toUri().toURL();
            urlClassLoader = new PublicURLClassLoader(new URL[]{jarURL});
            LogHelper.info("Check server jar MainClass");
            mainClassName = file.getManifest().getMainAttributes().getValue("Main-Class");
            agentClassName = file.getManifest().getMainAttributes().getValue("Premain-Class");
            if (mainClassName == null) {
                LogHelper.error("Main-Class not found in MANIFEST");
                return;
            }
            try {
                Class.forName(mainClassName, false, urlClassLoader);
            } catch (ClassNotFoundException e) {
                LogHelper.error(e);
                return;
            }
        }
        LogHelper.info("Found MainClass %s", mainClassName);
        if (agentClassName != null) {
            LogHelper.info("Found PremainClass %s", agentClassName);
        }
        System.out.println("Print your server name:");
        wrapper.config.serverName = commands.commandHandler.readLine();
        wrapper.config.mainclass = mainClassName;
        boolean stopOnError = wrapper.config.stopOnError;
        for (int i = 0; i < 10; ++i) {
            System.out.println("Print launchserver websocket host( ws://host:port/api ):");
            wrapper.config.address = commands.commandHandler.readLine();
            System.out.println("Print server token:");
            String checkServerToken = commands.commandHandler.readLine();
            wrapper.config.extendedTokens.put("checkServer", checkServerToken);
            wrapper.config.stopOnError = false;
            wrapper.updateLauncherConfig();
            try {
                wrapper.restore();
                wrapper.getProfiles();
            } catch (Throwable e) {
                LogHelper.error(e);
                Request.service.close();
            }
        }
        wrapper.config.stopOnError = stopOnError;
        wrapper.saveConfig();
        LogHelper.info("Generate start script");
        Path startScript;
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) startScript = Paths.get("start.bat");
        else startScript = Paths.get("start.sh");
        if (Files.exists(startScript)) {
            LogHelper.warning("start script found. Move to start.bak");
            Path startScriptBak = Paths.get("start.bak");
            IOHelper.move(startScript, startScriptBak);
        }
        try (Writer writer = IOHelper.newWriter(startScript)) {
            if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
                writer.append("#!/bin/bash\n\n");
            }
            writer.append(IOHelper.resolveJavaBin(Paths.get(System.getProperty("java.home"))).toAbsolutePath().toString());
            writer.append(" ");
            if (mainClassName.contains("bungee")) {
                LogHelper.info("Found BungeeCord mainclass. Modules dir change to modules_srv");
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
            if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
                writer.append(";");
            } else writer.append(":");
            writer.append(jarName);
            writer.append(" ");
            writer.append(ServerWrapper.class.getName());
            writer.append("\n");
        }
    }
}
