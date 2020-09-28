package pro.gravit.launchserver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import pro.gravit.launcher.LauncherInject;
import pro.gravit.launchserver.asm.InjectClassAcceptor;
import pro.gravit.utils.helper.JarHelper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ASMTransformersTest {
    public static ASMClassLoader classLoader;

    @BeforeAll
    public static void prepare() throws Throwable {
        classLoader = new ASMClassLoader(ASMTransformersTest.class.getClassLoader());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testASM() throws Throwable {
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
        Object instance = MethodHandles.publicLookup().findConstructor(clazz, MethodType.methodType(void.class)).invoke();
        Assertions.assertEquals(1234, (int)
                MethodHandles.publicLookup().findGetter(clazz, "test", int.class).invoke(instance));
        Assertions.assertEquals(strings, (List<String>)
                MethodHandles.publicLookup().findGetter(clazz, "s", List.class).invoke(instance));

        Assertions.assertEquals(byteMap, (Map<String, Object>)
                MethodHandles.publicLookup().findGetter(clazz, "map", Map.class).invoke(instance));
    }

    public static class ASMClassLoader extends ClassLoader {
        public ASMClassLoader(ClassLoader parent) {
            super(parent);
        }

        public void rawDefineClass(String name, byte[] bytes, int offset, int length) {
            defineClass(name, bytes, offset, length);
        }
    }

    public static class TestClass {
        @LauncherInject(value = "testprop")
        public int test;
        @LauncherInject(value = "testprop2")
        public List<String> s;
        @LauncherInject(value = "testprop3")
        public Map<String, String> map;
    }
}
