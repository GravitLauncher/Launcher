package ru.gravit.launchserver.components;

import ru.gravit.launcher.NeedGarbageCollection;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.components.Component;
import ru.gravit.launchserver.response.auth.AuthResponse;
import ru.gravit.launchserver.socket.Client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AuthLimiterComponent extends Component implements NeedGarbageCollection {
    @Override
    public void preInit(LaunchServer launchServer) {
    }

    @Override
    public void init(LaunchServer launchServer) {
        launchServer.authHookManager.registerPreHook(this::preAuthHook);
    }

    @Override
    public void postInit(LaunchServer launchServer) {

    }
    public void preAuthHook(AuthResponse.AuthContext context, Client client) throws AuthException {
        if(isLimit(context.ip))
        {
            AuthProvider.authError(message);
        }
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
}
