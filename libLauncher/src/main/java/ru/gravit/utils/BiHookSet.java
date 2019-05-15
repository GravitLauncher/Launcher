package ru.gravit.utils;

import java.util.HashSet;
import java.util.Set;

public class BiHookSet<V, R> {
    public Set<Hook<V, R>> list = new HashSet<>();

    @FunctionalInterface
    public interface Hook<V, R> {
        boolean hook(V object, R context) throws HookException;
    }

    public void registerHook(Hook<V, R> hook) {
        list.add(hook);
    }

    public boolean unregisterHook(Hook<V, R> hook) {
        return list.remove(hook);
    }

    public boolean hook(V context, R object) throws HookException {
        for (Hook<V, R> hook : list) {
            if (hook.hook(context, object)) return true;
        }
        return false;
    }
}
