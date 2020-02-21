package pro.gravit.launchserver.command.dump;

import com.google.gson.reflect.TypeToken;
import pro.gravit.launcher.Launcher;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DumpSessionsCommand extends Command {
    public DumpSessionsCommand(LaunchServer server) {
        super(server);
        childCommands.put("load", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                LogHelper.info("Sessions read from %s", args[0]);
                int size;
                try (Reader reader = IOHelper.newReader(Paths.get(args[0]))) {
                    Type setType = new TypeToken<HashSet<Client>>() {
                    }.getType();
                    Set<Client> clientSet = Launcher.gsonManager.configGson.fromJson(reader, setType);
                    size = clientSet.size();
                    server.sessionManager.loadSessions(clientSet);
                }
                LogHelper.subInfo("Readed %d sessions", size);
            }
        });
        childCommands.put("unload", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                LogHelper.info("Sessions write to %s", args[0]);
                Collection<Client> clientSet = server.sessionManager.getSessions();
                try (Writer writer = IOHelper.newWriter(Paths.get(args[0]))) {
                    Launcher.gsonManager.configGson.toJson(clientSet, writer);
                }
                LogHelper.subInfo("Write %d sessions", clientSet.size());
            }
        });
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
        invokeSubcommands(args);
    }
}
