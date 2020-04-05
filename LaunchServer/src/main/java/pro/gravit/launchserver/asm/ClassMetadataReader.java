package pro.gravit.launchserver.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import pro.gravit.utils.helper.IOHelper;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Позволяет искать методы внутри незагруженных классов и общие суперклассы для
 * чего угодно. Работает через поиск class-файлов в classpath.
 */
public class ClassMetadataReader implements Closeable {
    private final List<JarFile> cp;

    public ClassMetadataReader(List<JarFile> cp) {
        this.cp = cp;
    }

    public ClassMetadataReader() {
        this.cp = new ArrayList<>();
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
        return IOHelper.read(IOHelper.getResourceURL(className + ".class"));
    }

    public String getSuperClass(String type) {
        if (type.equals("java/lang/Object")) return null;
        try {
            return getSuperClassASM(type);
        } catch (Exception e) {
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
            super(Opcodes.ASM7);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            superClassName = superName;
        }
    }

}
