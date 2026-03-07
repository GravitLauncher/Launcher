package pro.gravit.launchserver.components;

import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.HookException;

public class AuthLimiterComponent extends IPLimiter implements AutoCloseable {
    private transient LaunchServer srv;

    @Override
    public void init(LaunchServer launchServer) {
        srv = launchServer;
        launchServer.authHookManager.preHook.registerHook(this::preAuthHook);
    }

    public boolean preAuthHook(AuthResponse.AuthContext context, Client client) {
        if (!check(context.ip)) {
            throw new HookException("auth.trylater");
        }
        return false;
    }

    @Override
    public void close() {
        srv.authHookManager.preHook.unregisterHook(this::preAuthHook);
    }
}
