package net.minecraftforge.fml;

import ru.gravit.utils.helper.JVMHelper;

// FMLSecurityManager запрещает делать System.exit из классов
// Не входящих в пакеты самого Forge
public class SafeExitJVM {
    public static void exit(int code) {
        try {
            JVMHelper.RUNTIME.halt(code);
        } catch (Throwable e) {
            System.exit(code);
        }
    }
}
