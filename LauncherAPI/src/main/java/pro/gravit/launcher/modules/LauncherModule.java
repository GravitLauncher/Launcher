package pro.gravit.launcher.modules;

import java.util.HashMap;
import java.util.Map;

public abstract class LauncherModule {
    private LauncherModulesContext context;

    private Map<Class<? extends Event>, EventHandler> eventMap = new HashMap<>();
    protected LauncherModulesManager modulesManager;
    protected final LauncherModuleInfo moduleInfo;
    protected ModulesConfigManager modulesConfigManager;
    protected InitPhase initPhase = InitPhase.CREATED;

    protected LauncherModule() {
        moduleInfo = new LauncherModuleInfo("UnknownModule");
    }

    public LauncherModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    public enum InitPhase
    {
        CREATED,
        INIT,
        FINISH
    }
    @FunctionalInterface
    public interface EventHandler<T extends Event>
    {
        void event(T e) throws Exception;
    }
    public static class Event
    {
        public boolean isCancel() {
            return cancel;
        }

        public Event cancel() {
            this.cancel = true;
            return this;
        }

        protected boolean cancel = false;
    }

    public InitPhase getInitPhase() {
        return initPhase;
    }

    public LauncherModule setInitPhase(InitPhase initPhase) {
        this.initPhase = initPhase;
        return this;
    }

    public void setContext(LauncherModulesContext context)
    {
        if(this.context != null) throw new IllegalStateException("Module already set context");
        this.context = context;
        this.modulesManager = context.getModulesManager();
        this.modulesConfigManager = context.getModulesConfigManager();
    }

    public void preInit() {
        //NOP
    }

    public abstract void init();


    protected <T extends Event> boolean registerEvent(EventHandler<T> handle, Class<T> tClass)
    {
        eventMap.put(tClass, handle);
        return true;
    }

    @SuppressWarnings("unchecked")
    public final <T extends Event> void callEvent(T event) throws Exception
    {
        Class<? extends Event> tClass = event.getClass();
        for(Map.Entry<Class<? extends Event>, EventHandler> e : eventMap.entrySet())
        {

            if(e.getKey().isAssignableFrom(tClass))
            {
                e.getValue().event(event);
                if(event.isCancel()) return;
            }
        }
    }
}
