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

    public static final long TIMEOUT = 12 * 60 * 60 * 1000; //12 часов
    public transient LaunchServer launchServer;
    public int rateLimit;
    public int rateLimitMilis;
    public String message;
    public transient HookSet.Hook<AuthHookManager.RegContext> hook;

    public transient HashMap<String, AuthLimiterComponent.AuthEntry> map = new HashMap<>();
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
        launchServer.authHookManager.registraion.registerHook(context -> {
            if (isLimit(context.ip)) {
                throw new HookException(message);
            }
            return false;
        });
    }

    @Override
    public void garbageCollection() {
        long time = System.currentTimeMillis();
        long max_timeout = Math.max(rateLimitMilis, TIMEOUT);
        map.entrySet().removeIf(e -> e.getValue().ts + max_timeout < time);
    }

    public boolean isLimit(String ip) {
        if (excludeIps.contains(ip)) return false;
        if (map.containsKey(ip)) {
            AuthLimiterComponent.AuthEntry rate = map.get(ip);
            long currenttime = System.currentTimeMillis();
            if (rate.ts + rateLimitMilis < currenttime) rate.value = 0;
            if (rate.value >= rateLimit && rateLimit > 0) {
                rate.value++;
                rate.ts = currenttime;
                return true;
            }
            rate.value++;
            rate.ts = currenttime;
            return false;
        }
        map.put(ip, new AuthLimiterComponent.AuthEntry(1, System.currentTimeMillis()));
        return false;
    }

    @Override
    public void close() throws Exception {
        if(hook != null)
            launchServer.authHookManager.registraion.unregisterHook(hook);
    }
}
