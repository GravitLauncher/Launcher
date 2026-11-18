package pro.gravit.launcher.base.modules.impl;

import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.core.LauncherTrustManager;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public abstract class ClasspathModuleManager extends SimpleModuleManager {
    protected ClasspathModuleManager(LauncherTrustManager trustManager, boolean fileLoading) {
        super(null, null, trustManager, fileLoading);
    }

    @Override
    public void autoload() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void autoload(Path dir) {
        throw new UnsupportedOperationException();
    }

    public List<LauncherModule> getModules() {
        return Collections.unmodifiableList(modules);
    }
}
