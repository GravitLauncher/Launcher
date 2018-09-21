package ru.gravit.launchserver.asm;

import org.objectweb.asm.*;

import ru.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Позволяет искать методы внутри незагруженных классов
 * и общие суперклассы для чего угодно. Работает через поиск class-файлов в classpath.
 */
public class ClassMetadataReader {
	private final List<JarFile> list;

    public ClassMetadataReader() {
    	this.list = new ArrayList<>();
    }
	
    public ClassMetadataReader(List<JarFile> list) {
    	this.list = new ArrayList<>(list);
    }
    
    public void add(JarFile jar) {
    	list.add(jar);
    }
    
    public byte[] getClassData(String className) throws IOException {
        String classResourceName = className.replace('.', '/') + ".class";
        for (JarFile jar : list) {
        	if (jar.getJarEntry(classResourceName) != null) return IOHelper.read(jar.getInputStream(jar.getJarEntry(classResourceName)));
        }
        return IOHelper.read(ClassMetadataReader.class.getResourceAsStream('/' + classResourceName));
    }

    public void acceptVisitor(byte[] classData, ClassVisitor visitor) {
        new ClassReader(classData).accept(visitor, 0);
    }

    public void acceptVisitor(String className, ClassVisitor visitor) throws IOException {
        acceptVisitor(getClassData(className), visitor);
    }

    public MethodReference findVirtualMethod(String owner, String name, String desc) {
        ArrayList<String> superClasses = getSuperClasses(owner);
        for (int i = superClasses.size() - 1; i > 0; i--) { // чекать текущий класс смысла нет
            String className = superClasses.get(i);
            MethodReference methodReference = getMethodReference(className, name, desc);
            if (methodReference != null) {
                System.out.println("found virtual method: " + methodReference);
                return methodReference;
            }
        }
        return null;
    }

    private MethodReference getMethodReference(String type, String methodName, String desc) {
        try {
            return getMethodReferenceASM(type, methodName, desc);
        } catch (Exception e) {
            return null;
        }
    }

    protected MethodReference getMethodReferenceASM(String type, String methodName, String desc) throws IOException {
        FindMethodClassVisitor cv = new FindMethodClassVisitor(methodName, desc);
        acceptVisitor(type, cv);
        if (cv.found) {
            return new MethodReference(type, cv.targetName, cv.targetDesc);
        }
        return null;
    }

    protected boolean checkSameMethod(String sourceName, String sourceDesc, String targetName, String targetDesc) {
        return sourceName.equals(targetName) && sourceDesc.equals(targetDesc);
    }

    /**
     * Возвращает суперклассы в порядке возрастающей конкретности (начиная с java/lang/Object
     * и заканчивая данным типом)
     */
    public ArrayList<String> getSuperClasses(String type) {
        ArrayList<String> superclasses = new ArrayList<String>(1);
        superclasses.add(type);
        while ((type = getSuperClass(type)) != null) {
            superclasses.add(type);
        }
        Collections.reverse(superclasses);
        return superclasses;
    }
    public String getSuperClass(String type) {
        try {
			return getSuperClassASM(type);
		} catch (Exception e) {
			return null;
		}
    }
    
    protected String getSuperClassASM(String type) throws IOException {
        CheckSuperClassVisitor cv = new CheckSuperClassVisitor();
        acceptVisitor(type, cv);
        return cv.superClassName;
    }

    private class CheckSuperClassVisitor extends ClassVisitor {

        String superClassName;

        public CheckSuperClassVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.superClassName = superName;
        }
    }

    protected class FindMethodClassVisitor extends ClassVisitor {

        public String targetName;
        public String targetDesc;
        public boolean found;

        public FindMethodClassVisitor(String name, String desc) {
            super(Opcodes.ASM5);
            this.targetName = name;
            this.targetDesc = desc;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            System.out.println("visiting " + name + "#" + desc);
            if ((access & Opcodes.ACC_PRIVATE) == 0 && checkSameMethod(name, desc, targetName, targetDesc)) {
                found = true;
                targetName = name;
                targetDesc = desc;
            }
            return null;
        }
    }

    public static class MethodReference {

        public final String owner;
        public final String name;
        public final String desc;

        public MethodReference(String owner, String name, String desc) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }

        public Type getType() {
            return Type.getMethodType(desc);
        }

        @Override public String toString() {
            return "MethodReference{" +
                    "owner='" + owner + '\'' +
                    ", name='" + name + '\'' +
                    ", desc='" + desc + '\'' +
                    '}';
        }
    }

}