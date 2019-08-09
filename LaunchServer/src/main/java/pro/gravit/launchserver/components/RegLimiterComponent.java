package pro.gravit.launchserver.components;

import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.manangers.hook.AuthHookManager;
import pro.gravit.utils.HookException;
import pro.gravit.utils.HookSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RegLimiterComponent extends Component implements NeedGarbageCollection, AutoCloseable {

    private transient AbstractLimiter<String> limiter;
    public transient LaunchServer launchServer;
    public int rateLimit;
    public int rateLimitMilis;
    public String message;

    public List<String> excludeIps = new ArrayList<>();

    @Override
    public void preInit(LaunchServer launchServer) {
        limiter = new AbstractLimiter<>(rateLimit, rateLimitMilis);
        this.launchServer = launchServer;
    }

    @Override
    public void init(LaunchServer launchServer) {

    }

    @Override
    public void postInit(LaunchServer launchServer) {
        launchServer.authHookManager.registraion.registerHook(this::registerHook);
    }

    public boolean registerHook(AuthHookManager.RegContext context)
    {
        if (!limiter.check(context.ip)) {
            throw new HookException(message);
        }
        return false;
    }

    @Override
    public void garbageCollection() {
        if(limiter != null)
            limiter.garbageCollection();
    }
    @Override
    public void close() throws Exception {
        launchServer.authHookManager.registraion.unregisterHook(this::registerHook);
    }
}
