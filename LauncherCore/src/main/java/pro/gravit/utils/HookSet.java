package pro.gravit.utils;

import java.util.HashSet;
import java.util.Set;

public class HookSet<R> {
    public final Set<Hook<R>> list = new HashSet<>();

    public void registerHook(Hook<R> hook) {
        list.add(hook);
    }

    public boolean unregisterHook(Hook<R> hook) {
        return list.remove(hook);
    }

    /**
     * @param context custom param
     * @return True if hook to interrupt processing
     * False to continue
     * @throws HookException The hook may return the error text throwing this exception
     */
    public boolean hook(R context) throws HookException {
        for (Hook<R> hook : list) {
            if (hook.hook(context)) return true;
        }
        return false;
    }

    @FunctionalInterface
    public interface Hook<R> {
        /**
         * @param context custom param
         * @return True if you need to interrupt hook processing
         * False to continue processing hook
         * @throws HookException The hook may return the error text throwing this exception
         */
        boolean hook(R context) throws HookException;
    }
}
