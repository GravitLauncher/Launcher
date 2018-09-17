package ru.gravit.launchserver.integration.plugin.bukkit;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import ru.gravit.launchserver.integration.plugin.LaunchServerPluginBridge;

public final class LaunchServerCommandBukkit implements CommandExecutor {
    public final LaunchServerPluginBukkit plugin;

    public LaunchServerCommandBukkit(LaunchServerPluginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        // Eval command
        LaunchServerPluginBridge bridge = plugin.bridge;
        if (bridge == null)
			sender.sendMessage(ChatColor.RED + LaunchServerPluginBridge.nonInitText);
		else
			bridge.eval(args);
        return true;
    }
}
