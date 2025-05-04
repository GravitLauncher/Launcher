package pro.gravit.launcher.server;

public class ServerWrapperInlineInitializer {
    public static void initialize() throws Exception {
        ServerWrapper.wrapper = new ServerWrapper(ServerWrapper.Config.class, ServerWrapper.configFile);
        ServerWrapper.wrapper.initialize();
        ServerWrapper.wrapper.connect();
    }
}
