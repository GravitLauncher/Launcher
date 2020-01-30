package pro.gravit.launchserver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import pro.gravit.launcher.LauncherInject;
import pro.gravit.launcher.LauncherInjectionConstructor;
import pro.gravit.launchserver.asm.InjectClassAcceptor;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.utils.helper.JarHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        public int test;
        @LauncherInject(value = "testprop2")
        public List<String> s;
        @LauncherInject(value = "testprop3")
        public Map<String, String> map;
    }
    @BeforeAll
    public static void prepare() throws Exception {
        classLoader = new ASMClassLoader(ASMTransformersTest.class.getClassLoader());
    }
    @Test
    void testASM() throws Exception
    {
        ClassReader reader = new ClassReader(JarHelper.getClassBytes(TestClass.class));
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.SKIP_DEBUG);
        node.name = "ASMTestClass";
        Map<String, Object> map = new HashMap<>();
        map.put("testprop", 1234);
        List<String> strings = new ArrayList<>();
        strings.add("a");
        strings.add("b");
        map.put("testprop2", strings);
        Map<String, String> byteMap = new HashMap<>();
        byteMap.put("a", "TEST A");
        byteMap.put("b", "TEST B");
        map.put("testprop3", byteMap);
        InjectClassAcceptor injectClassAcceptor = new InjectClassAcceptor(map);
        injectClassAcceptor.transform(node, "ASMTestClass", null);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        classLoader.rawDefineClass("ASMTestClass", bytes, 0, bytes.length);
        Class<?> clazz = classLoader.loadClass("ASMTestClass");
        Object instance = clazz.newInstance();
        Field field = clazz.getField("test");
        Object result = field.get(instance);
        Assertions.assertEquals(1234, (int) (Integer) result);
        field = clazz.getField("s");
        result = field.get(instance);
        Assertions.assertEquals(strings, result);
        field = clazz.getField("map");
        result = field.get(instance);
        Assertions.assertEquals(byteMap, result);
    }
}
