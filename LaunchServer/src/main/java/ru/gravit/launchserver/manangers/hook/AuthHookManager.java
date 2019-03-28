package ru.gravit.launchserver.manangers.hook;

import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.response.auth.AuthResponse;
import ru.gravit.launchserver.socket.Client;

import java.util.HashSet;
import java.util.Set;

public class AuthHookManager {
    private Set<AuthPreHook> PRE_HOOKS = new HashSet<>();
    private Set<AuthPostHook> POST_HOOKS = new HashSet<>();
    private Set<CheckServerHook> CHECKSERVER_HOOKS = new HashSet<>();
    private Set<JoinServerHook> JOINSERVER_HOOKS = new HashSet<>();

    @FunctionalInterface
    public interface AuthPreHook {
        void preAuthHook(AuthResponse.AuthContext context, Client client) throws AuthException;
    }

    @FunctionalInterface
    public interface AuthPostHook {
        void postAuthHook(AuthResponse.AuthContext context, Client client) throws AuthException;
    }

    @FunctionalInterface
    public interface CheckServerHook {
        void checkServerHook(String username, String serverID) throws AuthException;
    }

    @FunctionalInterface
    public interface JoinServerHook {
        void joinServerHook(String username, String accessToken, String serverID) throws AuthException;
    }

    public void registerPostHook(AuthPostHook hook) {
        POST_HOOKS.add(hook);
    }

    public void registerJoinServerHook(JoinServerHook hook) {
        JOINSERVER_HOOKS.add(hook);
    }

    public void registerCheckServerHook(CheckServerHook hook) {
        CHECKSERVER_HOOKS.add(hook);
    }

    public void registerPreHook(AuthPreHook hook) {
        PRE_HOOKS.add(hook);
    }

    public void preHook(AuthResponse.AuthContext context, Client client) throws AuthException {
        for (AuthPreHook preHook : PRE_HOOKS) {
            preHook.preAuthHook(context, client);
        }
    }

    public void checkServerHook(String username, String serverID) throws AuthException {
        for (CheckServerHook hook : CHECKSERVER_HOOKS) {
            hook.checkServerHook(username, serverID);
        }
    }

    public void joinServerHook(String username, String accessToken, String serverID) throws AuthException {
        for (JoinServerHook hook : JOINSERVER_HOOKS) {
            hook.joinServerHook(username, accessToken, serverID);
        }
    }

    public void postHook(AuthResponse.AuthContext context, Client client) throws AuthException {
        for (AuthPostHook postHook : POST_HOOKS) {
            postHook.postAuthHook(context, client);
        }
    }
}
