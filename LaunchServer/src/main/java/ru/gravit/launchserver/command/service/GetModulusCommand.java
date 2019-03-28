package ru.gravit.launchserver.command.service;
	
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.helper.LogHelper;

public class GetModulusCommand extends Command {
    public GetModulusCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return null;
    }

    @Override
    public void invoke(String... args) throws Exception {
        LogHelper.info("You publickey modulus: %s", LaunchServer.server.publicKey.getModulus().toString(16));
    }
}
