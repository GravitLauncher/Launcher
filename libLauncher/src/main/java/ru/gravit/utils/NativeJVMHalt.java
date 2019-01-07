package ru.gravit.utils;

import ru.gravit.utils.logging.LogHelper;

public class NativeJVMHalt {
    public NativeJVMHalt(int haltCode) {
        this.haltCode = haltCode;
        LogHelper.error("JVM exit code %d", haltCode);
        halt();
    }

    public int haltCode;

    public native void halt();
}
