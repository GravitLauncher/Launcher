package pro.gravit.launcher.base.modules;

import pro.gravit.utils.Version;

public class LauncherModuleInfoBuilder {
    private String name;
    private Version version;
    private String[] dependencies = new String[0];
    private int priority;
    private String[] providers = new String[0];

    public LauncherModuleInfoBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public LauncherModuleInfoBuilder setVersion(Version version) {
        this.version = version;
        return this;
    }

    public LauncherModuleInfoBuilder setDependencies(String[] dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public LauncherModuleInfoBuilder setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public LauncherModuleInfoBuilder setProviders(String[] providers) {
        this.providers = providers;
        return this;
    }

    public LauncherModuleInfo createLauncherModuleInfo() {
        return new LauncherModuleInfo(name, version, priority, dependencies, providers);
    }
}