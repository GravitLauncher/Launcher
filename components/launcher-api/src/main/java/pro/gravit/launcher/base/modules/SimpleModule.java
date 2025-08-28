package pro.gravit.launcher.base.modules;

import pro.gravit.utils.Version;

import java.util.function.Consumer;

public class SimpleModule extends LauncherModule {
    protected Consumer<SimpleModuleAccessor> consumer;

    public SimpleModule(LauncherModuleInfo info, Consumer<SimpleModuleAccessor> consumer) {
        super(info);
        this.consumer = consumer;
    }

    @Override
    public void init(LauncherInitContext initContext) {
        consumer.accept(new SimpleModuleAccessor(initContext));
    }

    public static SimpleModule of(String name, Version version, Consumer<SimpleModuleAccessor> consumer) {
        return new SimpleModule(new LauncherModuleInfoBuilder()
                .setName(name)
                .setVersion(version)
                .createLauncherModuleInfo(), consumer);
    }

    public class SimpleModuleAccessor {
        private final LauncherInitContext context;

        public SimpleModuleAccessor(LauncherInitContext context) {
            this.context = context;
        }


        public final <T extends LauncherModule> T requireModule(Class<? extends T> clazz, Version minVersion) {
            return SimpleModule.this.requireModule(clazz, minVersion);
        }

        public final void requireModule(String name, Version minVersion) {
            SimpleModule.this.requireModule(name, minVersion);
        }

        public final LauncherModulesContext getModulesContext() {
            return SimpleModule.this.getContext();
        }

        public LauncherInitContext getContext() {
            return context;
        }

        public  <T extends Event> boolean registerEvent(EventHandler<T> handle, Class<T> tClass) {
            return SimpleModule.this.registerEvent(handle, tClass);
        }

        public final <T extends Event> void callEvent(T event) {
            SimpleModule.this.callEvent(event);
        }
    }
}
