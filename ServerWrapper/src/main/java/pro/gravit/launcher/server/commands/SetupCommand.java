package pro.gravit.launcher.server.commands;

import pro.gravit.launcher.events.request.GetPublicKeyRequestEvent;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.auth.GetPublicKeyRequest;
import pro.gravit.launcher.request.websockets.StdWebSocketService;
import pro.gravit.launcher.server.ServerWrapper;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.File;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

public class SetupCommand extends Command {
    private final ServerWrapper wrapper;

    public SetupCommand(ServerWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Setup ServerWrapper";
    }

    @Override
    public void invoke(String... args) throws Exception {
        LogHelper.info("Print server jar filename:");
        String jarName = this.wrapper.commandHandler.readLine();
        Path jarPath = Paths.get(jarName);

        String agentClassName;

        try (JarFile file = new JarFile(jarPath.toFile())) {
            LogHelper.info("Check server jar MainClass");
            this.wrapper.config.mainclass = file.getManifest().getMainAttributes().getValue("Main-Class");
            agentClassName = file.getManifest().getMainAttributes().getValue("Premain-Class");

            if (this.wrapper.config.mainclass == null) {
                LogHelper.error("Main-Class not found in MANIFEST");
                return;
            }

            try {
                Class.forName(this.wrapper.config.mainclass, false, new PublicURLClassLoader(new URL[] { jarPath.toUri().toURL() }));
            } catch (ClassNotFoundException e) {
                LogHelper.error(e);
                return;
            }
        }

        LogHelper.info("Found MainClass %s", this.wrapper.config.mainclass);
        if (agentClassName != null)
            LogHelper.info("Found PremainClass %s", agentClassName);

        for (int i = 0; i < 10; ++i) {
            if(!Request.isAvailable() || Request.getRequestService().isClosed()) {
                LogHelper.info("Print websocket address (ws://host:port/api):");

                wrapper.config.address = this.wrapper.commandHandler.readLine();

                try {
                    Request.setRequestService(StdWebSocketService.initWebSockets(this.wrapper.config.address).get());
                } catch (Throwable e) {
                    LogHelper.error(e);
                    continue;
                }
            }

            LogHelper.info("Print server token:");
            this.wrapper.config.extendedTokens.put("checkServer", this.wrapper.commandHandler.readLine());

            try {
                this.wrapper.restore();

                GetPublicKeyRequestEvent publicKeyRequestEvent = new GetPublicKeyRequest().request();
                this.wrapper.config.encodedServerRsaPublicKey = publicKeyRequestEvent.rsaPublicKey;
                this.wrapper.config.encodedServerEcPublicKey = publicKeyRequestEvent.ecdsaPublicKey;
                break;
            } catch (Throwable e) {
                LogHelper.error(e);

                if (Request.isAvailable() && Request.getRequestService() instanceof AutoCloseable) {
                    ((AutoCloseable) Request.getRequestService()).close();
                }
            }
        }

        LogHelper.info("(ONLY FOR 1.18+) Use alternative start mode? (Y/N)");
        boolean altMode = this.wrapper.commandHandler.readLine().equals("Y");

        if (altMode) {
            LogHelper.debug("Switch to alternative start mode (1.18+)");

            this.wrapper.config.classpath.add(jarName);
            this.wrapper.config.classLoaderConfig = ClientProfile.ClassLoaderConfig.LAUNCHER;
        }

        this.wrapper.saveConfig();

        LogHelper.info("Generate start script");
        Path startScript = Paths.get(JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE ? "start.bat" : "start.sh");

        if (Files.exists(startScript)) {
            LogHelper.warning("Start script found. Move to start.bak");
            IOHelper.move(startScript, Paths.get("start.bak"));
        }

        try (Writer writer = IOHelper.newWriter(startScript)) {
            if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
                writer.append("#!/bin/bash");
                writer.append(System.lineSeparator());
                writer.append(System.lineSeparator());
            }

            writer.append(System.lineSeparator());
            writer.append(IOHelper.resolveJavaBin(Paths.get(System.getProperty("java.home")), true).toAbsolutePath().toString());
            writer.append(System.lineSeparator());
            writer.append(" ");

            if (agentClassName != null) {
                writer.append("-javaagent:ServerWrapper.jar ");
                writer.append("-Dserverwrapper.agentproxy=");
                writer.append(agentClassName);
                writer.append(" ");
            }

            writer.append("-Dserverwrapper.disableSetup=true -cp ");
            writer.append(IOHelper.getCodeSource(ServerWrapper.class).getFileName().toString());

            if(!altMode) {
                writer.append(File.pathSeparator);
                writer.append(jarName);
            }

            writer.append(" ");
            writer.append(ServerWrapper.class.getName());
            writer.append(System.lineSeparator());
        }

        if (JVMHelper.OS_TYPE != JVMHelper.OS.MUSTDIE) {
            if (!startScript.toFile().setExecutable(true)) {
                LogHelper.error("Failed to set executable %s", startScript);
            }
        }
    }
}
