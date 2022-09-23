package pro.gravit.launcher.utils;

import pro.gravit.utils.helper.LogHelper;

import java.lang.reflect.Method;

public final class NativeJVMHalt {
    public final int haltCode;

    public NativeJVMHalt(int haltCode) {
        this.haltCode = haltCode;
        System.out.println("JVM exit code " + haltCode);
    }

    public static void initFunc() {

    }

    public static void haltA(int code) {
        Throwable[] th = new Throwable[3];
        NativeJVMHalt halt = new NativeJVMHalt(code);
        try {
            LogHelper.dev("Try invoke Shutdown.exit");
            Class<?> clazz = Class.forName("java.lang.Shutdown", true, ClassLoader.getSystemClassLoader());
            Method exitMethod = clazz.getDeclaredMethod("exit", int.class);
            exitMethod.setAccessible(true);
            exitMethod.invoke(null, code);
        } catch (Throwable e) {
            th[1] = e;
            if (LogHelper.isDevEnabled()) {
                LogHelper.error(e);
            }
        }
    }
}
