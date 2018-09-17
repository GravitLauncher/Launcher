package ru.gravit.launchserver.integration.plugin.bungee;

import ru.gravit.launchserver.integration.plugin.LaunchServerPluginBridge;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
//import net.md_5.bungee.command.ConsoleCommandSender;

public final class LaunchServerCommandBungee extends Command {
    private static final BaseComponent[] NOT_INITIALIZED_MESSAGE = TextComponent.fromLegacyText(ChatColor.RED + LaunchServerPluginBridge.nonInitText);

    // Instance
    public final LaunchServerPluginBungee plugin;

    public LaunchServerCommandBungee(LaunchServerPluginBungee plugin) {
        super("launchserver", LaunchServerPluginBridge.perm, "ru/gravit/launcher", "ls", "l");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Eval command
        LaunchServerPluginBridge bridge = plugin.bridge;
        if (bridge == null)
			sender.sendMessage(NOT_INITIALIZED_MESSAGE);
		else
			bridge.eval(args);
    }
}
