package ru.gravit.launchserver.command.dump;

import com.google.gson.reflect.TypeToken;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class DumpSessionsCommand extends Command {
    public DumpSessionsCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[load/unload] [filename]";
    }

    @Override
    public String getUsageDescription() {
        return "Load or unload sessions";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        if (args[0].equals("unload")) {
            LogHelper.info("Sessions write to %s", args[1]);
            Set<Client> clientSet = server.sessionManager.getSessions();
            try (Writer writer = IOHelper.newWriter(Paths.get(args[1]))) {
                LaunchServer.gson.toJson(clientSet, writer);
            }
            LogHelper.subInfo("Write %d sessions", clientSet.size());
        } else if (args[0].equals("load")) {
            LogHelper.info("Sessions read from %s", args[1]);
            int size = 0;
            try (Reader reader = IOHelper.newReader(Paths.get(args[1]))) {
                Type setType = new TypeToken<HashSet<Client>>() {
                }.getType();
                Set<Client> clientSet = LaunchServer.gson.fromJson(reader, setType);
                size = clientSet.size();
                server.sessionManager.loadSessions(clientSet);
            }
            LogHelper.subInfo("Readed %d sessions", size);
        }
    }
}
