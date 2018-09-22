package ru.gravit.launcher.transformers;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CodeConverter;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import ru.gravit.utils.PublicURLClassLoader;

public class SystemClassLoaderTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader classLoader, String classname, Class<?> aClass, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {
    	if(classname.startsWith("ru/gravit/launcher/")) return bytes;
        if(classname.startsWith("java/")) return bytes;
        if(classname.startsWith("sun/")) return bytes;
        if(classname.startsWith("com/sun/")) return bytes;
        if(classname.startsWith("javax/")) return bytes;
        if(classname.startsWith("jdk/")) return bytes;
        try {
            ClassPool pool = ClassPool.getDefault();
            pool.appendClassPath(new LoaderClassPath(PublicURLClassLoader.systemclassloader));
            pool.appendClassPath(new LoaderClassPath(classLoader));
            CtClass s1 = pool.get("java.lang.ClassLoader");
            CtMethod m11 = s1.getDeclaredMethod("getSystemClassLoader"); // Находим метод, который нам нужно заменить
            CtClass s2 = pool.get(PublicURLClassLoader.class.getName());
            CtMethod m21 = s2.getDeclaredMethod("getSystemClassLoader"); // Находим метод, на который мы будем заменять
            CodeConverter cc = new CodeConverter();
            cc.redirectMethodCall(m11, m21); // Указываем что на что нам нужно заменить

            CtClass cl = pool.makeClass(new ByteArrayInputStream(bytes), false); // Загружаем класс, переданный для трансформации
            if(cl.isFrozen()) return null;
            CtConstructor[] constructors = cl.getConstructors(); // Находим все конструкторы класса
            for(CtConstructor constructor : constructors)
				constructor.instrument(cc); // Заменяем вызовы
            CtMethod[] methods = cl.getDeclaredMethods(); // Находим все методы класса
            for(CtMethod method : methods)
				method.instrument(cc); // Заменяем вызовы
            return cl.toBytecode();
        } catch (Exception ex) {
        	
        }
        return bytes;
    }
}
