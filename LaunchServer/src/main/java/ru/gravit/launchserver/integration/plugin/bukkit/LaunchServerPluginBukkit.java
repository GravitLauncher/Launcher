package ru.gravit.launchserver.integration.plugin.bukkit;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import ru.gravit.launchserver.integration.plugin.LaunchServerPluginBridge;

public final class LaunchServerPluginBukkit extends JavaPlugin {
    public volatile LaunchServerPluginBridge bridge = null;

    @Override
    public void onDisable() {
        super.onDisable();
        if (bridge != null) {
            bridge.close();
            bridge = null;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

        // Initialize LaunchServer
        try {
            bridge = new LaunchServerPluginBridge(getDataFolder().toPath());
        } catch (Throwable exc) {
            exc.printStackTrace();
        }

        // Register command
        PluginCommand com = getCommand("launchserver");
        com.setPermission(LaunchServerPluginBridge.perm);
        com.setExecutor(new LaunchServerCommandBukkit(this));
    }
}
