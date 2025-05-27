package pro.gravit.launcher.runtime.debug;

public class DebugMainInlineInitializer {
    public static void run() throws Exception {
        DebugMain.initialize();
        DebugMain.authorize();
    }
}
