package ru.gravit.launchserver.command.service;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.provider.AcceptAuthProvider;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.auth.provider.RejectAuthProvider;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.helper.LogHelper;

public class SwapAuthProviderCommand extends Command {
    public AuthProvider[] providersCache;
    public SwapAuthProviderCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[index] [accept/reject/undo] [message(for reject)]";
    }

    @Override
    public String getUsageDescription() {
        return "Change authProvider";
    }

    @SuppressWarnings("resource")
	@Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args,2);
        if(providersCache == null) providersCache = new AuthProvider[server.config.authProvider.length];
        int index = Integer.valueOf(args[0]);
        switch (args[1]) {
            case "accept":
                if (providersCache[index] == null) {
                    AcceptAuthProvider provider = new AcceptAuthProvider();
                    providersCache[index] = server.config.authProvider[index];
                    server.config.authProvider[index] = provider;
                    LogHelper.info("AuthProvider[%d] is AcceptAuthProvider", index);
                } else LogHelper.error("Changes detected. Use undo");
                break;
            case "reject":
                if (providersCache[index] == null) {
                    RejectAuthProvider rejectAuthProvider;
                    if (args.length < 3) rejectAuthProvider = new RejectAuthProvider();
                    else rejectAuthProvider = new RejectAuthProvider(args[2]);
                    providersCache[index] = server.config.authProvider[index];
                    server.config.authProvider[index] = rejectAuthProvider;
                    LogHelper.info("AuthProvider[%d] is RejectAuthProvider", index);
                } else LogHelper.error("Changes detected. Use undo");
                break;
            case "undo":
                if (providersCache[index] == null) LogHelper.error("Cache clean. Undo impossible");
                else {
                    server.config.authProvider[index].close();
                    server.config.authProvider[index] = providersCache[index];
                    providersCache[index] = null;
                }

                break;
        }
    }
}
