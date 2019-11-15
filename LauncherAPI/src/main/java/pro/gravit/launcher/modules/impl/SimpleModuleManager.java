package pro.gravit.launcher.modules.impl;

import pro.gravit.launcher.managers.SimpleModulesConfigManager;
import pro.gravit.launcher.modules.*;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.verify.LauncherTrustManager;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarFile;

public class SimpleModuleManager implements LauncherModulesManager {
    private static final MethodType VOID_TYPE = MethodType.methodType(void.class);
    protected final List<LauncherModule> modules = new ArrayList<>();
    protected final List<String> moduleNames = new ArrayList<>();
    protected final SimpleModuleContext context;
    protected final ModulesConfigManager modulesConfigManager;
    protected final Path modulesDir;
    protected final LauncherTrustManager trustManager;
    protected LauncherInitContext initContext;
    protected LauncherTrustManager.CheckMode checkMode = LauncherTrustManager.CheckMode.WARN_IN_NOT_SIGNED;

    protected final PublicURLClassLoader classLoader = new PublicURLClassLoader(new URL[]{});

    protected final class ModulesVisitor extends SimpleFileVisitor<Path> {
        private ModulesVisitor() {
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toFile().getName().endsWith(".jar"))
                loadModule(file);
            return super.visitFile(file, attrs);
        }
    }

    public void autoload() throws IOException {
        autoload(modulesDir);
    }

    public void autoload(Path dir) throws IOException {
        if (Files.notExists(dir)) Files.createDirectory(dir);
        else {
            IOHelper.walk(dir, new ModulesVisitor(), true);
        }
    }

    public void initModules(LauncherInitContext initContext) {
        boolean isAnyModuleLoad = true;
        modules.sort((m1, m2) -> {
            int priority1 = m1.getModuleInfo().priority;
            int priority2 = m2.getModuleInfo().priority;
            return Integer.compare(priority1, priority2);
        });
        while (isAnyModuleLoad) {
            isAnyModuleLoad = false;
            for (LauncherModule module : modules) {
                if (!module.getInitStatus().equals(LauncherModule.InitStatus.INIT_WAIT)) continue;
                if (checkDepend(module)) {
                    isAnyModuleLoad = true;
                    module.setInitStatus(LauncherModule.InitStatus.INIT);
                    module.init(initContext);
                    module.setInitStatus(LauncherModule.InitStatus.FINISH);
                }
            }
        }
        for (LauncherModule module : modules) {
            if (module.getInitStatus().equals(LauncherModule.InitStatus.INIT_WAIT)) {
                LauncherModuleInfo info = module.getModuleInfo();
                LogHelper.warning("Module %s required %s. Cyclic dependencies?", info.name, Arrays.toString(info.dependencies));
                module.setInitStatus(LauncherModule.InitStatus.INIT);
                module.init(initContext);
                module.setInitStatus(LauncherModule.InitStatus.FINISH);
            } else if (module.getInitStatus().equals(LauncherModule.InitStatus.PRE_INIT_WAIT)) {
                LauncherModuleInfo info = module.getModuleInfo();
                LogHelper.error("Module %s skip pre-init phase. This module NOT finish loading", info.name, Arrays.toString(info.dependencies));
            }
        }
    }

    private boolean checkDepend(LauncherModule module) {
        LauncherModuleInfo info = module.getModuleInfo();
        for (String dep : info.dependencies) {
            LauncherModule depModule = getModule(dep);
            if (depModule == null)
                throw new RuntimeException(String.format("Module %s required %s. %s not found", info.name, dep, dep));
            if (!depModule.getInitStatus().equals(LauncherModule.InitStatus.FINISH)) return false;
        }
        return true;
    }

    public SimpleModuleManager(Path modulesDir, Path configDir) {
        modulesConfigManager = new SimpleModulesConfigManager(configDir);
        context = new SimpleModuleContext(this, modulesConfigManager);
        this.modulesDir = modulesDir;
        this.trustManager = null;
    }

    public SimpleModuleManager(Path modulesDir, Path configDir, LauncherTrustManager trustManager) {
        modulesConfigManager = new SimpleModulesConfigManager(configDir);
        context = new SimpleModuleContext(this, modulesConfigManager);
        this.modulesDir = modulesDir;
        this.trustManager = trustManager;
    }

    @Override
    public LauncherModule loadModule(LauncherModule module) {
        if (modules.contains(module)) return module;
        modules.add(module);
        LauncherModuleInfo info = module.getModuleInfo();
        moduleNames.add(info.name);
        module.setContext(context);
        module.preInit();
        if (initContext != null) {
            module.setInitStatus(LauncherModule.InitStatus.INIT);
            module.init(initContext);
            module.setInitStatus(LauncherModule.InitStatus.FINISH);
        }
        return module;
    }

    @Override
    public LauncherModule loadModule(Path file) throws IOException {
        try (JarFile f = new JarFile(file.toFile())) {
            String moduleClass = f.getManifest().getMainAttributes().getValue("Module-Main-Class");
            if (moduleClass == null) {
                LogHelper.error("In module %s Module-Main-Class not found", file.toString());
                return null;
            }
            classLoader.addURL(file.toUri().toURL());
            @SuppressWarnings("unchecked")
            Class<? extends LauncherModule> clazz = (Class<? extends LauncherModule>) Class.forName(moduleClass, false, classLoader);
            checkModuleClass(clazz, checkMode);
            if (!LauncherModule.class.isAssignableFrom(clazz))
            	throw new ClassNotFoundException("Invalid module class... Not contains LauncherModule in hierarchy.");
            LauncherModule module;
            try {
            	module = (LauncherModule) MethodHandles.publicLookup().findConstructor(clazz, VOID_TYPE).invoke();
            } catch (Throwable e) {
            	throw (InstantiationException) new InstantiationException("Error on instancing...").initCause(e);
            }
            loadModule(module);
            return module;
        } catch (ClassNotFoundException | InstantiationException e) {
            LogHelper.error(e);
            LogHelper.error("In module %s Module-Main-Class incorrect", file.toString());
            return null;
        }
    }


    public void checkModuleClass(Class<? extends LauncherModule> clazz, LauncherTrustManager.CheckMode mode) throws SecurityException {
        if (trustManager == null) return;
        X509Certificate[] certificates = JVMHelper.getCertificates(clazz);
        if (certificates == null) {
            if (mode == LauncherTrustManager.CheckMode.EXCEPTION_IN_NOT_SIGNED)
                throw new SecurityException(String.format("Class %s not signed", clazz.getName()));
            else if (mode == LauncherTrustManager.CheckMode.WARN_IN_NOT_SIGNED)
                LogHelper.warning("Class %s not signed", clazz.getName());
            return;
        }
        try {
            trustManager.checkCertificate(certificates, (c, s) -> {

            });
        } catch (CertificateException | NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new SecurityException(e);
        }
    }

    @Override
    public LauncherModule getModule(String name) {
        for (LauncherModule module : modules) {
            LauncherModuleInfo info = module.getModuleInfo();
            if (info.name.equals(name) || (info.providers.length > 0 && Arrays.asList(info.providers).contains(name)))
                return module;
        }
        return null;
    }

    @Override
    public LauncherModule getCoreModule() {
        return null;
    }

    @Override
    public ClassLoader getModuleClassLoader() {
        return classLoader;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LauncherModule> T getModule(Class<? extends T> clazz) {
        for (LauncherModule module : modules) {
            if (clazz.isAssignableFrom(module.getClass())) return (T) module;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getModuleByInterface(Class<T> clazz) {
        for (LauncherModule module : modules) {
            if (clazz.isAssignableFrom(module.getClass())) return (T) module;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getModulesByInterface(Class<T> clazz) {
        List<T> list = new ArrayList<>();
        for (LauncherModule module : modules) {
            if (clazz.isAssignableFrom(module.getClass())) list.add((T) module);
        }
        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LauncherModule> T findModule(Class<? extends T> clazz, Predicate<Version> versionPredicate) {
        for (LauncherModule module : modules) {
            LauncherModuleInfo info = module.getModuleInfo();
            if (!versionPredicate.test(info.version)) continue;
            if (clazz.isAssignableFrom(module.getClass())) return (T) module;
        }
        return null;
    }

    @Override
    public <T extends LauncherModule.Event> void invokeEvent(T event) {
        for (LauncherModule module : modules) {
            module.callEvent(event);
            if (event.isCancel()) return;
        }
    }

    @Override
    public ModulesConfigManager getConfigManager() {
        return modulesConfigManager;
    }
}
