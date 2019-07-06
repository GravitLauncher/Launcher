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
	private transient final Hook<AuthResponse.AuthContext, Client> prA = this::preAuthHook;
	private transient LaunchServer srv;
    @Override
    public void preInit(LaunchServer launchServer) {
    	srv = launchServer;
    }

    @Override
    public void init(LaunchServer launchServer) {
        launchServer.authHookManager.preHook.registerHook(prA);
    }

    @Override
    public void postInit(LaunchServer launchServer) {

    }

    public boolean preAuthHook(AuthResponse.AuthContext context, Client client) {
        if (isLimit(context.ip)) {
            throw new HookException(message);
        }
        return false;
    }

    static class AuthEntry {
        public int value;

        public long ts;

        public AuthEntry(int i, long l) {
            value = i;
            ts = l;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof AuthEntry))
                return false;
            AuthEntry other = (AuthEntry) obj;
            if (ts != other.ts)
                return false;
            return value == other.value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (ts ^ ts >>> 32);
            result = prime * result + value;
            return result;
        }

        @Override
        public String toString() {
            return String.format("AuthEntry {value=%s, ts=%s}", value, ts);
        }
    }


    public static final long TIMEOUT = 10 * 60 * 1000; //10 минут
    public int rateLimit;
    public int rateLimitMilis;
    public String message;

    public transient HashMap<String, AuthEntry> map = new HashMap<>();
    public List<String> excludeIps = new ArrayList<>();

    @Override
    public void garbageCollection() {
        long time = System.currentTimeMillis();
        long max_timeout = Math.max(rateLimitMilis, TIMEOUT);
        map.entrySet().removeIf(e -> e.getValue().ts + max_timeout < time);
    }

    public boolean isLimit(String ip) {
        if (excludeIps.contains(ip)) return false;
        if (map.containsKey(ip)) {
            AuthEntry rate = map.get(ip);
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
        map.put(ip, new AuthEntry(1, System.currentTimeMillis()));
        return false;
    }

	@Override
	public void close() throws Exception {
        srv.authHookManager.preHook.unregisterHook(prA);
	}
}
