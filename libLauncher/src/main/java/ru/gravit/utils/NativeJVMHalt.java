package ru.gravit.utils;

import ru.gravit.utils.helper.LogHelper;

public final class NativeJVMHalt {
    public NativeJVMHalt(int haltCode) {
        this.haltCode = haltCode;
        LogHelper.error("JVM exit code %d", haltCode);
        halt();
    }

    public int haltCode;

    public native void halt();
}
