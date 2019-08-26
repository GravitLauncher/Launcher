package pro.gravit.launcher.modules;

import java.util.HashMap;
import java.util.Map;

public abstract class LauncherModule {
    private LauncherModulesContext context;

    private Map<Class<? extends Event>, EventHandler> eventMap = new HashMap<>();
    protected LauncherModulesManager modulesManager;
    protected ModulesConfigManager modulesConfigManager;
    InitPhase initPhase = InitPhase.CREATED;
    public enum InitPhase
    {
        CREATED,
        INIT,
        FINISH
    }
    public enum EventAction
    {
        CONTINUE,
        INTERRUPT
    }
    @FunctionalInterface
    public interface EventHandler<T extends Event>
    {
        EventAction event(T e) throws Exception;
    }
    public interface Event
    {

    }

    public InitPhase getInitPhase() {
        return initPhase;
    }

    Map<Class<? extends Event>, EventHandler> setContext(LauncherModulesContext context)
    {
        if(this.context != null) throw new IllegalStateException("Module already set context");
        this.context = context;
        this.modulesManager = context.getModulesManager();
        this.modulesConfigManager = context.getModulesConfigManager();
        return eventMap;
    }
    public abstract LauncherModuleInfo init();

    <T extends Event> boolean registerEvent(EventHandler<T> handle, Class<T> tClass)
    {
        eventMap.put(tClass, handle);
        return true;
    }

    <T extends Event> EventAction callEvent(T event) throws Exception
    {
        Class<? extends Event> tClass = event.getClass();
        for(Map.Entry<Class<? extends Event>, EventHandler> e : eventMap.entrySet())
        {

            if(e.getKey().isAssignableFrom(tClass))
            {
                @SuppressWarnings("unchecked")
                EventAction action = e.getValue().event(event);
                if(action.equals(EventAction.INTERRUPT)) return action;
            }
        }
        return EventAction.CONTINUE;
    }
}
