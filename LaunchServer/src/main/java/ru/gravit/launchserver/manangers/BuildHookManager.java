package ru.gravit.launchserver.manangers;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import ru.gravit.launcher.AutogenConfig;
import ru.gravit.launcher.modules.TestClientModule;
import ru.gravit.launchserver.binary.BuildContext;
import ru.gravit.launchserver.binary.JAConfigurator;

public class BuildHookManager {
    @FunctionalInterface
    public interface PostBuildHook {
        void build(BuildContext context);
    }

    @FunctionalInterface
    public interface PreBuildHook {
        void build(BuildContext context);
    }
    @FunctionalInterface
    public interface ProGuardTransformHook {
        byte[] transform(byte[] input, CharSequence classname);
    }

    @FunctionalInterface
    public interface Transformer {
        byte[] transform(byte[] input, CharSequence classname);
    }

    private boolean BUILDRUNTIME;
    private final Set<PostBuildHook> POST_HOOKS;
    private final Set<ProGuardTransformHook> POST_PROGUARD_HOOKS;
    private final Set<PreBuildHook> PRE_HOOKS;
    private final Set<Transformer> CLASS_TRANSFORMER;
    private final Set<String> CLASS_BLACKLIST;
    private final Set<String> MODULE_CLASS;
    private final Map<String, byte[]> INCLUDE_CLASS;

    public BuildHookManager() {
        POST_HOOKS = new HashSet<>(4);
        POST_PROGUARD_HOOKS = new HashSet<>(4);
        PRE_HOOKS = new HashSet<>(4);
        CLASS_BLACKLIST = new HashSet<>(4);
        MODULE_CLASS = new HashSet<>(4);
        INCLUDE_CLASS = new HashMap<>(4);
        CLASS_TRANSFORMER = new HashSet<>(4);
        BUILDRUNTIME = true;
        autoRegisterIgnoredClass(AutogenConfig.class.getName());
        registerIgnoredClass("META-INF/DEPENDENCIES");
        registerIgnoredClass("META-INF/LICENSE");
        registerIgnoredClass("META-INF/NOTICE");
        registerClientModuleClass(TestClientModule.class.getName());
    }

    public void autoRegisterIgnoredClass(String clazz) {
        CLASS_BLACKLIST.add(clazz.replace('.', '/').concat(".class"));
    }

    public boolean buildRuntime() {
        return BUILDRUNTIME;
    }

    public byte[] classTransform(byte[] clazz, CharSequence classname) {
        byte[] result = clazz;
        for (Transformer transformer : CLASS_TRANSFORMER) result = transformer.transform(result, classname);
        return result;
    }
    public byte[] proGuardClassTransform(byte[] clazz, CharSequence classname) {
        byte[] result = clazz;
        for (ProGuardTransformHook transformer : POST_PROGUARD_HOOKS) result = transformer.transform(result, classname);
        return result;
    }

    public void registerIncludeClass(String classname, byte[] classdata) {
        INCLUDE_CLASS.put(classname, classdata);
    }

    public Map<String, byte[]> getIncludeClass() {
        return INCLUDE_CLASS;
    }

    public boolean isContainsBlacklist(String clazz) {
        return CLASS_BLACKLIST.contains(clazz);
    }

    public void postHook(BuildContext context) {
        for (PostBuildHook hook : POST_HOOKS) hook.build(context);
    }

    public void preHook(BuildContext context) {
        for (PreBuildHook hook : PRE_HOOKS) hook.build(context);
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

    public void registerPostHook(PostBuildHook hook) {
        POST_HOOKS.add(hook);
    }
    public void registerProGuardHook(ProGuardTransformHook hook) {
        POST_PROGUARD_HOOKS.add(hook);
    }
    public boolean isNeedPostProguardHook()
    {
        return !POST_PROGUARD_HOOKS.isEmpty();
    }

    public void registerPreHook(PreBuildHook hook) {
        PRE_HOOKS.add(hook);
    }

    public void setBuildRuntime(boolean runtime) {
        BUILDRUNTIME = runtime;
    }
}
