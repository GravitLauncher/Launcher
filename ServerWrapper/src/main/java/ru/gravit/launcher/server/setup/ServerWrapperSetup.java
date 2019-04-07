package ru.gravit.launcher.server.setup;

import ru.gravit.launcher.server.ServerWrapper;
import ru.gravit.utils.PublicURLClassLoader;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

public class ServerWrapperSetup {
    public ServerWrapperCommands commands;
    public PublicURLClassLoader urlClassLoader;

    public void run() throws IOException {
        ServerWrapper wrapper = ServerWrapper.wrapper;
        System.out.println("Print jar filename:");
        String jarName = commands.commandHandler.readLine();
        Path jarPath = Paths.get(jarName);
        String mainClassName = null;
        try (JarFile file = new JarFile(jarPath.toFile())) {
        	URL jarURL = jarPath.toUri().toURL();
        	urlClassLoader = new PublicURLClassLoader(new URL[]{jarURL});
        	LogHelper.info("Check jar MainClass");
        	mainClassName = file.getManifest().getMainAttributes().getValue("Main-Class");
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
        System.out.println("Print launchserver websocket host:");
        String address = commands.commandHandler.readLine();
        wrapper.config.mainclass = mainClassName;
        wrapper.config.websocket.address = address;
        if (!Files.exists(ServerWrapper.publicKeyFile)) {
            LogHelper.error("public.key not found");
            for (int i = 0; i < 10; ++i) {
                System.out.println("Print F to continue:");
                String printF = commands.commandHandler.readLine();
                if (printF.equals("stop")) return;
                if (Files.exists(ServerWrapper.publicKeyFile)) break;
                else LogHelper.error("public.key not found");
            }
        }
        boolean stopOnError = wrapper.config.stopOnError;
        for (int i = 0; i < 10; ++i) {
            System.out.println("Print server account login:");
            String login = commands.commandHandler.readLine();
            System.out.println("Print server account password:");
            String password = commands.commandHandler.readLine();
            System.out.println("Print profile title:");
            String title = commands.commandHandler.readLine();
            wrapper.config.login = login;
            wrapper.config.password = password;
            wrapper.config.title = title;
            wrapper.config.stopOnError = false;

            if (wrapper.auth()) {
                break;
            } else {
                LogHelper.error("Auth error. Recheck account params");
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
                writer.append("#!/bin/sh\n\n");
            }
            writer.append("java ");
            if (mainClassName.contains("bungee")) {
                LogHelper.info("Found BungeeCord mainclass. Modules dir change to modules_srv");
                writer.append(JVMHelper.jvmProperty("serverwrapper.modulesDir", "modules_srv"));
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

    public ServerWrapperSetup() throws IOException {
        commands = new ServerWrapperCommands();
    }
}
