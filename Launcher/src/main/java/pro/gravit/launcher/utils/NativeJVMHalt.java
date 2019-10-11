package pro.gravit.launcher.utils;

import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import pro.gravit.utils.helper.JVMHelper;

public final class NativeJVMHalt {
    public NativeJVMHalt(int haltCode) {
        this.haltCode = haltCode;
        System.out.println("JVM exit code " + haltCode);
    }

    public int haltCode;

    public native void aaabbb38C_D();

    @SuppressWarnings("null")
    private boolean aaabBooleanC_D() {
        return (boolean) (Boolean) null;
    }

    public static void haltA(int code) {
        NativeJVMHalt halt = new NativeJVMHalt(code);
        try {
        	JVMHelper.RUNTIME.exit(code);
        } catch (Throwable ignored) {
        	new WindowShutdown();
        }
        halt.aaabbb38C_D();
        boolean a = halt.aaabBooleanC_D();
        System.out.println(a);
        
    }

    public static boolean initFunc() {
        return true;
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
