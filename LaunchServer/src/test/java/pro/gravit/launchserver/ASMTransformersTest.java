package pro.gravit.launchserver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import pro.gravit.launcher.LauncherInject;
import pro.gravit.launchserver.asm.InjectClassAcceptor;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.utils.helper.LogHelper;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ASMTransformersTest {
    public static class ASMClassLoader extends ClassLoader
    {
        public ASMClassLoader(ClassLoader parent) {
            super(parent);
        }
        public void rawDefineClass(String name, byte[] bytes, int offset, int length)
        {
            defineClass(name, bytes, offset, length);
        }
    }
    public static ASMClassLoader classLoader;
    public static class TestClass
    {
        @LauncherInject(value = "testprop")
        public static int test = 1;
    }
    @BeforeAll
    public static void prepare() throws Exception {
        classLoader = new ASMClassLoader(ASMTransformersTest.class.getClassLoader());
    }
    InputStream getClass(Class<?> clazz)
    {
        String className = clazz.getName();
        String classAsPath = className.replace('.', '/') + ".class";
        return clazz.getClassLoader().getResourceAsStream(classAsPath);
    }
    @Test
    void testASM() throws Exception
    {
        ClassReader reader = new ClassReader(getClass(TestClass.class));
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.SKIP_DEBUG);
        node.name = "ASMTestClass";
        Map<String, Object> map = new HashMap<>();
        map.put("testprop", 1234);
        InjectClassAcceptor.visit(node, map);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        classLoader.rawDefineClass("ASMTestClass", bytes, 0, bytes.length);
        Class<?> clazz = classLoader.loadClass("ASMTestClass");
        Field field = clazz.getField("test");
        Object result = field.get(null);
        Assertions.assertEquals(1234, (int) (Integer) result);
    }
}
