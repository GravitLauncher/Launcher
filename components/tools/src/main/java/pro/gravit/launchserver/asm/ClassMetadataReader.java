package pro.gravit.launchserver.asm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import pro.gravit.utils.helper.IOHelper;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarFile;

/**
 * Позволяет искать методы внутри незагруженных классов и общие суперклассы для
 * чего угодно. Работает через поиск class-файлов в classpath.
 */
public class ClassMetadataReader implements Closeable {
    private final Logger logger = LogManager.getLogger(ClassMetadataReader.class);
    private final List<JarFile> cp;
    private final Map<String, Module> moduleClassFinder;

    public ClassMetadataReader(List<JarFile> cp) {
        this.cp = cp;
        //var moduleLayer = ClassMetadataReader.class.getModule().getLayer() == null ? ModuleLayer.boot() : ClassMetadataReader.class.getModule().getLayer();
        var moduleLayer = ModuleLayer.boot();
        moduleClassFinder = collectModulePackages(moduleLayer);
    }

    public ClassMetadataReader() {
        this.cp = new ArrayList<>();
        //var moduleLayer = ClassMetadataReader.class.getModule().getLayer() == null ? ModuleLayer.boot() : ClassMetadataReader.class.getModule().getLayer();
        var moduleLayer = ModuleLayer.boot();
        moduleClassFinder = collectModulePackages(moduleLayer);
    }

    private Map<String, Module> collectModulePackages(ModuleLayer layer) {
        var map = new HashMap<String, Module>();
        for(var m : layer.modules()) {
            for(var p : m.getPackages()) {
                map.put(p, m);
            }
        }
        return map;
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
        if(ClassMetadataReader.class.getModule().isNamed()) {
            String pkg = getClassPackage(className).replace('/', '.');
            var module = moduleClassFinder.get(pkg);
            if(module != null) {
                var cl = module.getClassLoader();
                if(cl == null) {
                    cl = ClassLoader.getPlatformClassLoader();
                }
                var stream = cl.getResourceAsStream(className+".class");
                if(stream != null) {
                    try(stream) {
                        return IOHelper.read(stream);
                    }
                } else {
                    throw new FileNotFoundException("Class "+className + ".class");
                }
            } else {
                throw new FileNotFoundException("Package "+pkg);
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
