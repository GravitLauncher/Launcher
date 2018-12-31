package ru.gravit.launchserver.manangers.hook;

import ru.gravit.launcher.request.RequestException;
import ru.gravit.launchserver.socket.SocketContext;

import java.util.HashSet;
import java.util.Set;

public class SocketHookManager {
    @FunctionalInterface
    interface SocketPreHook
    {
        boolean preHook(SocketContext context); //Вернуть true если необходимо продолжть обработку, false если остановить обработку
    }
    @FunctionalInterface
    interface SocketPostHook
    {
        void postHook(SocketContext context);
    }
    @FunctionalInterface
    interface SocketErrorHook
    {
        boolean errorHook(SocketContext context, RequestException e); //Вернуть true если необходимо продолжть обработку, false если остановить обработку
    }
    private Set<SocketPostHook> POST_HOOKS;
    private Set<SocketPreHook> PRE_HOOKS;
    private Set<SocketErrorHook> ERROR_HOOKS;
    public void registerPostHook(SocketPostHook hook)
    {
        if(POST_HOOKS == null) POST_HOOKS = new HashSet<>();
        POST_HOOKS.add(hook);
    }
    public void registerPreHook(SocketPreHook hook)
    {
        if(PRE_HOOKS == null) PRE_HOOKS = new HashSet<>();
        PRE_HOOKS.add(hook);
    }
    public void registerErrorHook(SocketErrorHook hook)
    {
        if(ERROR_HOOKS == null) ERROR_HOOKS = new HashSet<>();
        ERROR_HOOKS.add(hook);
    }
    public boolean preHook(SocketContext context)
    {
        if(PRE_HOOKS == null) return true;
        for(SocketPreHook preHook : PRE_HOOKS)
        {
            if(!preHook.preHook(context)) return false;
        }
        return true;
    }
    public void postHook(SocketContext context)
    {
        if(POST_HOOKS == null) return;
        for(SocketPostHook postHook : POST_HOOKS)
        {
            postHook.postHook(context);
        }
    }
    public boolean errorHook(SocketContext context, RequestException e)
    {
        if(ERROR_HOOKS == null) return true;
        for(SocketErrorHook errorHook : ERROR_HOOKS)
        {
            if(!errorHook.errorHook(context,e)) return false;
        }
        return true;
    }
}
