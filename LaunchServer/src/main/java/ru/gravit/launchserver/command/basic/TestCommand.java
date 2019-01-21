package ru.gravit.launchserver.command.basic;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.launchserver.socket.NettyServerSocketHandler;
import ru.gravit.utils.helper.CommonHelper;

public class TestCommand extends Command {
    public TestCommand(LaunchServer server) {
        super(server);
    }

    private NettyServerSocketHandler handler = null;

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
        if (args[0].equals("stop")) {
            handler.close();
        }
    }
}
