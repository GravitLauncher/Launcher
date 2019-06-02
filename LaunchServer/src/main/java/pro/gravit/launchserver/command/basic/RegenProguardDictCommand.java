package pro.gravit.launchserver.command.basic;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

import java.io.IOException;

public class RegenProguardDictCommand extends Command {

    public RegenProguardDictCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Regenerates proguard dictonary";
    }

    @Override
    public void invoke(String... args) throws IOException {
        server.proguardConf.genWords(true);
    }

}
