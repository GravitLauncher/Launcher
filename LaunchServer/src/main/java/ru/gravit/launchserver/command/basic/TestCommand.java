package ru.gravit.launchserver.command.basic;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.launchserver.socket.NettyServerSocketHandler;

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
        verifyArgs(args,1);
        if(handler == null)
        handler = new NettyServerSocketHandler(server);
        if(args[0].equals("start"))
        {
            handler.run();
        }
    }
}
