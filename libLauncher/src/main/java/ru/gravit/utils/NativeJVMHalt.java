package ru.gravit.utils;

import cpw.mods.fml.SafeExitJVMLegacy;
import net.minecraftforge.fml.SafeExitJVM;

public final class NativeJVMHalt {
    public NativeJVMHalt(int haltCode) {
        this.haltCode = haltCode;
        System.out.println("JVM exit code " + haltCode);
    }

    public int haltCode;

    public native void aaabbb38C_D();

    @SuppressWarnings("null")
	private boolean aaabBooleanC_D() {
    	return (boolean) (Boolean) (Object) null;
    }
    
    public static void haltA(int code) {
        NativeJVMHalt halt = new NativeJVMHalt(code);
        try {
        	SafeExitJVMLegacy.exit(code);
    	} catch(Throwable ignored) {
    	}
        try {
        	SafeExitJVM.exit(code);
    	} catch(Throwable ignored) {
    	}
    	halt.aaabbb38C_D();
        boolean a = halt.aaabBooleanC_D();
        System.out.println(Boolean.toString(a));
    }
    
    public static boolean initFunc() {
    	return true;
    }
}
