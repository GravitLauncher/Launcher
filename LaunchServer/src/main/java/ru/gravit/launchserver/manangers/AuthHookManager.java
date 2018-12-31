package ru.gravit.launchserver.manangers;

import ru.gravit.launchserver.response.auth.AuthResponse;
import ru.gravit.launchserver.socket.Client;

import java.util.HashSet;
import java.util.Set;

public class AuthHookManager {
    private Set<AuthPreHook> PRE_HOOKS = new HashSet<>();
    private Set<AuthPostHook> POST_HOOKS = new HashSet<>();
    @FunctionalInterface
    public interface AuthPreHook
    {
        void preAuthHook(AuthResponse.AuthContext context, Client client);
    }
    public interface AuthPostHook
    {
        void postAuthHook(AuthResponse.AuthContext context, Client client);
    }
    public void registerPostHook(AuthPostHook hook)
    {
        if(POST_HOOKS == null) POST_HOOKS = new HashSet<>();
        POST_HOOKS.add(hook);
    }
    public void registerPreHook(AuthPreHook hook)
    {
        if(PRE_HOOKS == null) PRE_HOOKS = new HashSet<>();
        PRE_HOOKS.add(hook);
    }
    public void preHook(AuthResponse.AuthContext context, Client client)
    {
        for(AuthPreHook preHook : PRE_HOOKS)
        {
            preHook.preAuthHook(context,client);
        }
    }
    public void postHook(AuthResponse.AuthContext context, Client client)
    {
        for(AuthPostHook postHook : POST_HOOKS)
        {
            postHook.postAuthHook(context, client);
        }
    }
}
