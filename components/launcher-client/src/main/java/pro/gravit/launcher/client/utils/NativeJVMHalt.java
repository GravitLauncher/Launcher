package pro.gravit.launcher.client.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public final class NativeJVMHalt {

    private static final Logger logger =
            LoggerFactory.getLogger(NativeJVMHalt.class);

    public final int haltCode;

    public NativeJVMHalt(int haltCode) {
        this.haltCode = haltCode;
        System.out.println("JVM exit code " + haltCode);
    }

    public static void initFunc() {

    }

    public static void haltA(int code) {
        NativeJVMHalt halt = new NativeJVMHalt(code);
        try {
            logger.info("Try invoke Shutdown.exit");
            Class<?> clazz = Class.forName("java.lang.Shutdown", true, ClassLoader.getSystemClassLoader());
            Method exitMethod = clazz.getDeclaredMethod("exit", int.class);
            exitMethod.setAccessible(true);
            exitMethod.invoke(null, code);
        } catch (Throwable e) {
            if (true) {
                logger.error("", e);
            }
        }
    }
}