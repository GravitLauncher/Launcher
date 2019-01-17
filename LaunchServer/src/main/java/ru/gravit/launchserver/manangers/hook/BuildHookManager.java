package ru.gravit.launchserver.manangers.hook;

import ru.gravit.launcher.AutogenConfig;
import ru.gravit.launchserver.binary.BuildContext;
import ru.gravit.launchserver.binary.JAConfigurator;
import ru.gravit.launchserver.binary.tasks.MainBuildTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BuildHookManager {

    @FunctionalInterface
    public interface BuildHook {
        void build(BuildContext context);
    }

    @FunctionalInterface
    public interface Transformer {
        byte[] transform(byte[] input, String classname, MainBuildTask data);
    }

    private boolean BUILDRUNTIME;
    private final Set<BuildHook> HOOKS;
    private final Set<Transformer> CLASS_TRANSFORMER;
    private final Set<String> CLASS_BLACKLIST;
    private final Set<String> MODULE_CLASS;
    private final Map<String, byte[]> INCLUDE_CLASS;

    public BuildHookManager() {
        HOOKS = new HashSet<>(4);
        CLASS_BLACKLIST = new HashSet<>(4);
        MODULE_CLASS = new HashSet<>(4);
        INCLUDE_CLASS = new HashMap<>(4);
        CLASS_TRANSFORMER = new HashSet<>(4);
        BUILDRUNTIME = true;
        autoRegisterIgnoredClass(AutogenConfig.class.getName());
        registerIgnoredClass("META-INF/DEPENDENCIES");
        registerIgnoredClass("META-INF/LICENSE");
        registerIgnoredClass("META-INF/NOTICE");
    }

    public void autoRegisterIgnoredClass(String clazz) {
        CLASS_BLACKLIST.add(clazz.replace('.', '/').concat(".class"));
    }

    public boolean buildRuntime() {
        return BUILDRUNTIME;
    }

    public byte[] classTransform(byte[] clazz, String classname, MainBuildTask reader) {
        byte[] result = clazz;
        for (Transformer transformer : CLASS_TRANSFORMER) result = transformer.transform(result, classname, reader);
        return result;
    }

    public void registerIncludeClass(String classname, byte[] classdata) {
        INCLUDE_CLASS.put(classname, classdata);
    }

    public Map<String, byte[]> getIncludeClass() {
        return INCLUDE_CLASS;
    }

    public boolean isContainsBlacklist(String clazz) {
        for (String classB : CLASS_BLACKLIST) {
            if (clazz.startsWith(classB)) return true;
        }
        return false;
    }

    public void hook(BuildContext context) {
        for (BuildHook hook : HOOKS) hook.build(context);
    }

    public void registerAllClientModuleClass(JAConfigurator cfg) {
        for (String clazz : MODULE_CLASS) cfg.addModuleClass(clazz);
    }

    public void registerClassTransformer(Transformer transformer) {
        CLASS_TRANSFORMER.add(transformer);
    }

    public void registerClientModuleClass(String clazz) {
        MODULE_CLASS.add(clazz);
    }

    public void registerIgnoredClass(String clazz) {
        CLASS_BLACKLIST.add(clazz);
    }

    public void registerHook(BuildHook hook) {
        HOOKS.add(hook);
    }

    public void setBuildRuntime(boolean runtime) {
        BUILDRUNTIME = runtime;
    }
}
