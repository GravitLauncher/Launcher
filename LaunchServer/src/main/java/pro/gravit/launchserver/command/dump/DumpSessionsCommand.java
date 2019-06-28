package pro.gravit.launchserver.command.dump;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.reflect.TypeToken;

import pro.gravit.launcher.Launcher;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

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
                Launcher.gsonManager.configGson.toJson(clientSet, writer);
            }
            LogHelper.subInfo("Write %d sessions", clientSet.size());
        } else if (args[0].equals("load")) {
            LogHelper.info("Sessions read from %s", args[1]);
            int size;
            try (Reader reader = IOHelper.newReader(Paths.get(args[1]))) {
                Type setType = new TypeToken<HashSet<Client>>() {
                }.getType();
                Set<Client> clientSet = Launcher.gsonManager.configGson.fromJson(reader, setType);
                size = clientSet.size();
                server.sessionManager.loadSessions(clientSet);
            }
            LogHelper.subInfo("Readed %d sessions", size);
        }
    }
}
