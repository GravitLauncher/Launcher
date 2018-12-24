package ru.gravit.launchserver.command.dump;

import com.google.gson.reflect.TypeToken;
import javafx.scene.media.MediaException;
import ru.gravit.launcher.Launcher;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.IOHelper;

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
        verifyArgs(args,2);
        if(args[0].equals("unload"))
        {
            try(Writer writer = IOHelper.newWriter(Paths.get(args[1])))
            {
                Launcher.gson.toJson(server.sessionManager.getSessions(),writer);
            }
        } else if(args[0].equals("load"))
        {
            try(Reader reader = IOHelper.newReader(Paths.get(args[1])))
            {
                Type setType = new TypeToken<HashSet<Client>>(){}.getType();
                server.sessionManager.loadSessions(Launcher.gson.fromJson(reader,setType));
            }
        }
    }
}
