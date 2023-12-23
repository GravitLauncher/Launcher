package pro.gravit.utils.helper;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class HackHelper {
    private static native MethodHandles.Lookup createHackLookupNative(Class<?> lookupClass);
    private static MethodHandles.Lookup createHackLookupImpl(Class<?> lookupClass) {
        try {
            return createHackLookupNative(lookupClass);
        } catch (Throwable ignored) {

        }
        try {
            Field trusted = MethodHandles.Lookup.class.getDeclaredField("TRUSTED");
            trusted.setAccessible(true);
            int value = (int) trusted.get(null);
            Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Class.class,int.class);
            constructor.setAccessible(true);
            return constructor.newInstance(lookupClass, null, value);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodHandles.Lookup createHackLookup(Class<?> lookupClass) {
        Exception e = new Exception();
        StackTraceElement[] elements = e.getStackTrace();
        String className = elements[elements.length-1].getClassName();
        if(!className.startsWith("pro.gravit.launcher.") && !className.startsWith("pro.gravit.utils.")) {
            throw new SecurityException(String.format("Untrusted class %s", className));
        }
        return createHackLookupImpl(lookupClass);
    }
}
