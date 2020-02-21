package pro.gravit.launcher.client.events;

import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.utils.command.CommandHandler;

public class ClientUnlockConsoleEvent extends LauncherModule.Event {
    public final CommandHandler handler;

    public ClientUnlockConsoleEvent(CommandHandler handler) {
        this.handler = handler;
    }
}
