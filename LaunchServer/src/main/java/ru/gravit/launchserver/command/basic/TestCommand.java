package ru.gravit.launchserver.command.basic;

import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.launchserver.socket.NettyServerSocketHandler;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;

import java.io.Writer;

public class TestCommand extends Command {
    public TestCommand(LaunchServer server) {
        super(server);
    }

    NettyServerSocketHandler handler;

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Test command. Only developer!";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        if (handler == null)
            handler = new NettyServerSocketHandler(server);
        if (args[0].equals("start")) {
            CommonHelper.newThread("Netty Server", true, handler).start();
        }
        if (args[0].equals("profile")) {
            ClientProfile profile = new ClientProfile("1.7.10", "asset1.7.10", 0, "Test1.7.10", "Test server.", "localhost", 25565, true, false, "net.minecraft.launchwrapper.Launch");

            try (Writer writer = IOHelper.newWriter(server.dir.resolve("profiles").resolve("Test.cfg"))) {
                LaunchServer.gson.toJson(profile, writer);
            }


        }
        if (args[0].equals("stop")) {
            handler.close();
        }
    }
}
