package pro.gravit.launchserver.components;

import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.manangers.hook.AuthHookManager;
import pro.gravit.utils.HookException;

import java.util.ArrayList;
import java.util.List;

public class RegLimiterComponent extends IPLimiter implements NeedGarbageCollection, AutoCloseable {

    public transient LaunchServer launchServer;
    public String message;

    public List<String> excludeIps = new ArrayList<>();

    @Override
    public void preInit(LaunchServer launchServer) {
        this.launchServer = launchServer;
    }

    @Override
    public void init(LaunchServer launchServer) {

    }

    @Override
    public void postInit(LaunchServer launchServer) {
        launchServer.authHookManager.registraion.registerHook(this::registerHook);
    }

    public boolean registerHook(AuthHookManager.RegContext context) {
        if (!check(context.ip)) {
            throw new HookException(message);
        }
        return false;
    }

    @Override
    public void close() {
        launchServer.authHookManager.registraion.unregisterHook(this::registerHook);
    }
}
