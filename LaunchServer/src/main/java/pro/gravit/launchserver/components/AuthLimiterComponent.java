package pro.gravit.launchserver.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.BiHookSet.Hook;
import pro.gravit.utils.HookException;

public class AuthLimiterComponent extends Component implements NeedGarbageCollection, AutoCloseable {

	private transient AbstractLimiter<String> limiter;
	private transient LaunchServer srv;
    @Override
    public void preInit(LaunchServer launchServer) {
    	srv = launchServer;
    }

    @Override
    public void init(LaunchServer launchServer) {
        limiter = new AbstractLimiter<>(rateLimit, rateLimitMilis);
        launchServer.authHookManager.preHook.registerHook(this::preAuthHook);
    }

    @Override
    public void postInit(LaunchServer launchServer) {

    }

    public boolean preAuthHook(AuthResponse.AuthContext context, Client client) {
        if (!excludeIps.contains(context.ip) && !limiter.check(context.ip)) {
            throw new HookException(message);
        }
        return false;
    }

    public int rateLimit;
    public int rateLimitMilis;
    public String message;
    public List<String> excludeIps = new ArrayList<>();

    @Override
    public void garbageCollection() {
        if(limiter != null)
            limiter.garbageCollection();
    }

	@Override
	public void close() throws Exception {
        srv.authHookManager.preHook.unregisterHook(this::preAuthHook);
	}
}
