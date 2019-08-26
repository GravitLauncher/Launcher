package pro.gravit.launcher.modules;

import java.util.HashMap;
import java.util.Map;

public abstract class LauncherModule {
    private LauncherModulesContext context;

    private Map<Class<? extends Event>, EventHandler> eventMap = new HashMap<>();
    protected LauncherModulesManager modulesManager;
    protected final LauncherModuleInfo moduleInfo;
    protected ModulesConfigManager modulesConfigManager;
    protected InitStatus initStatus = InitStatus.CREATED;

    protected LauncherModule() {
        moduleInfo = new LauncherModuleInfo("UnknownModule");
    }

    protected LauncherModule(LauncherModuleInfo info) {
        moduleInfo = info;
    }

    public LauncherModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    public enum InitStatus
    {
        CREATED,
        INIT,
        FINISH
    }
    @FunctionalInterface
    public interface EventHandler<T extends Event>
    {
        void event(T e);
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

    public InitStatus getInitStatus() {
        return initStatus;
    }

    public LauncherModule setInitStatus(InitStatus initStatus) {
        this.initStatus = initStatus;
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

    public abstract void init(LauncherInitContext initContext);


    protected <T extends Event> boolean registerEvent(EventHandler<T> handle, Class<T> tClass)
    {
        eventMap.put(tClass, handle);
        return true;
    }

    @SuppressWarnings("unchecked")
    public final <T extends Event> void callEvent(T event)
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
