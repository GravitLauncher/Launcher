package ru.gravit.utils;

import java.util.HashSet;
import java.util.Set;

public class HookSet<R> {
    public Set<Hook<R>> list = new HashSet<>();
    @FunctionalInterface
    public interface Hook<R>
    {
        boolean hook(R context);
    }
    public void registerHook(Hook<R> hook)
    {
        list.add(hook);
    }
    public boolean unregisterHook(Hook<R> hook)
    {
        return list.remove(hook);
    }
    public boolean hook(R context)
    {
        for(Hook<R> hook : list)
        {
            if(hook.hook(context)) return true;
        }
        return false;
    }
}
