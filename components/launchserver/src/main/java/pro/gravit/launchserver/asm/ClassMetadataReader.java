package pro.gravit.launchserver.asm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JavaHelper;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;

/**
 * Позволяет искать методы внутри незагруженных классов и общие суперклассы для
 * чего угодно. Работает через поиск class-файлов в classpath.
 */
public class ClassMetadataReader implements Closeable {
    private final Logger logger = LogManager.getLogger(ClassMetadataReader.class);
    private final List<JarFile> cp;
    private final Map<String, ModuleReference> moduleClassFinder = new HashMap<>();
    private final Map<ModuleReference, ModuleReader> moduleReaders = new HashMap<>();

    public ClassMetadataReader(List<JarFile> cp) {
        this(cp, IOHelper.JVM_DIR);
    }

    ClassMetadataReader(List<JarFile> cp, Path javaHome) {
        this.cp = cp;
        ModuleFinder.ofSystem().findAll().forEach(this::collectModulePackages);
        // JavaFX can be shipped as separate JARs in the JDK, outside the runtime image.
        List<Path> modulePaths = new ArrayList<>();
        for (String moduleName : JavaHelper.javaFxModules) {
            Path modulePath = JavaHelper.tryFindModule(javaHome, moduleName);
            if (modulePath != null) {
                modulePaths.add(modulePath);
            }
        }
        ModuleFinder.of(modulePaths.toArray(Path[]::new)).findAll().forEach(this::collectModulePackages);
        var moduleLayer = ClassMetadataReader.class.getModule().getLayer();
        collectLayerPackages(moduleLayer == null ? ModuleLayer.boot() : moduleLayer, new HashSet<>());
    }

    public ClassMetadataReader() {
        this(new ArrayList<>());
    }

    private void collectModulePackages(ModuleReference module) {
        for (var pkg : module.descriptor().packages()) {
            moduleClassFinder.put(pkg, module);
        }
    }

    private void collectLayerPackages(ModuleLayer layer, Set<ModuleLayer> visited) {
        if (!visited.add(layer)) return;
        for (var parent : layer.parents()) {
            collectLayerPackages(parent, visited);
        }
        layer.configuration().modules().forEach(module -> collectModulePackages(module.reference()));
    }

    public List<JarFile> getCp() {
        return cp;
    }

    public void acceptVisitor(byte[] classData, ClassVisitor visitor) {
        new ClassReader(classData).accept(visitor, 0);
    }

    public void acceptVisitor(String className, ClassVisitor visitor) throws IOException {
        acceptVisitor(getClassData(className), visitor);
    }

    public void acceptVisitor(byte[] classData, ClassVisitor visitor, int flags) {
        new ClassReader(classData).accept(visitor, flags);
    }

    public void acceptVisitor(String className, ClassVisitor visitor, int flags) throws IOException {
        acceptVisitor(getClassData(className), visitor, flags);
    }

    public byte[] getClassData(String className) throws IOException {
        for (JarFile f : cp) {
            if (f.getEntry(className + ".class") != null) {
                byte[] bytes;
                try (InputStream in = f.getInputStream(f.getEntry(className + ".class"))) {
                    bytes = IOHelper.read(in);
                }
                return bytes;
            }
        }
        String pkg = getClassPackage(className).replace('/', '.');
        var module = moduleClassFinder.get(pkg);
        if (module != null) {
            var reader = moduleReaders.get(module);
            if (reader == null) {
                reader = module.open();
                moduleReaders.put(module, reader);
            }
            try (var stream = reader.open(className + ".class").orElse(null)) {
                if (stream != null) {
                    return IOHelper.read(stream);
                }
            }
        }
        var stream = ClassLoader.getSystemClassLoader().getResourceAsStream(className+".class");
        if(stream != null) {
            try(stream) {
                return IOHelper.read(stream);
            }
        } else {
            throw new FileNotFoundException(className + ".class");
        }
    }

    private String getClassPackage(String type) {
        int idx = type.lastIndexOf("/");
        if(idx <= 0) {
            return type;
        }
        return type.substring(0, idx);
    }

    public String getSuperClass(String type) {
        if (type.equals("java/lang/Object")) return null;
        try {
            return getSuperClassASM(type);
        } catch (Exception e) {
            logger.warn("getSuperClass: type {} not found ({}: {})", type, e.getClass().getName(), e.getMessage());
            return "java/lang/Object";
        }
    }

    protected String getSuperClassASM(String type) throws IOException {
        CheckSuperClassVisitor cv = new CheckSuperClassVisitor();
        acceptVisitor(type, cv);
        return cv.superClassName;
    }

    /**
     * Возвращает суперклассы в порядке возрастающей конкретности (начиная с
     * java/lang/Object и заканчивая данным типом)
     */
    public ArrayList<String> getSuperClasses(String type) {
        ArrayList<String> superclasses = new ArrayList<>(1);
        superclasses.add(type);
        while ((type = getSuperClass(type)) != null)
            superclasses.add(type);
        Collections.reverse(superclasses);
        return superclasses;
    }

    @Override
    public void close() {
        cp.forEach(IOHelper::close);
        cp.clear();
        moduleReaders.values().forEach(IOHelper::close);
        moduleReaders.clear();
    }

    private static class CheckSuperClassVisitor extends ClassVisitor {

        String superClassName;

        public CheckSuperClassVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            superClassName = superName;
        }
    }

}
