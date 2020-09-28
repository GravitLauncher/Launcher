package pro.gravit.launchserver.auth.protect;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.ProviderMap;

public abstract class ProtectHandler {
    public static final ProviderMap<ProtectHandler> providers = new ProviderMap<>("ProtectHandler");
    private static boolean registredHandl = false;


    public static void registerHandlers() {
        if (!registredHandl) {
            providers.register("none", NoProtectHandler.class);
            providers.register("std", StdProtectHandler.class);
            providers.register("advanced", AdvancedProtectHandler.class);
            registredHandl = true;
        }
    }

    public abstract boolean allowGetAccessToken(AuthResponse.AuthContext context);

    public abstract void checkLaunchServerLicense(); //Выдает SecurityException при ошибке проверки лицензии

    public void init(LaunchServer server) {

    }

    public void close() {

    }
    //public abstract
}
