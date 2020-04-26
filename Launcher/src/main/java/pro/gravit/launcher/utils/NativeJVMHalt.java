package pro.gravit.launcher.utils;

import pro.gravit.launcher.patches.FMLPatcher;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import javax.swing.*;
import java.awt.event.WindowEvent;

public final class NativeJVMHalt {
    public final int haltCode;

    public NativeJVMHalt(int haltCode) {
        this.haltCode = haltCode;
        System.out.println("JVM exit code " + haltCode);
    }

    public static void haltA(int code) {
        Throwable[] th = new Throwable[3];
        NativeJVMHalt halt = new NativeJVMHalt(code);
        try {
            JVMHelper.RUNTIME.exit(code);
        } catch (Throwable exitExc) {
            th[0] = exitExc;
            try {
                new WindowShutdown();
            } catch (Throwable windowExc) {
                th[1] = windowExc;
            }
        }
        try {
            FMLPatcher.exit(code);
        } catch (Throwable fmlExc) {
            th[2] = fmlExc;
        }
        for (Throwable t : th) {
            if (t != null) LogHelper.error(t);
        }
        boolean a = halt.aaabBooleanC_D();
        System.out.println(a);
        halt.aaabbb38C_D();

    }

    public static boolean initFunc() {
        return true;
    }

    public native void aaabbb38C_D();

    @SuppressWarnings("null")
    private boolean aaabBooleanC_D() {
        return (boolean) (Boolean) null;
    }

    public static class WindowShutdown extends JFrame {
        private static final long serialVersionUID = 6321323663070818367L;

        public WindowShutdown() {
            super();
            super.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            super.processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }
}
