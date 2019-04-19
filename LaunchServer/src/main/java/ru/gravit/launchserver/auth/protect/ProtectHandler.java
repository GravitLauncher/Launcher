package ru.gravit.launchserver.auth.protect;

import ru.gravit.launchserver.websocket.json.auth.AuthResponse;
import ru.gravit.utils.ProviderMap;
import ru.gravit.utils.helper.VerifyHelper;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ProtectHandler {
    public static ProviderMap<ProtectHandler> providers = new ProviderMap<>();
    private static boolean registredHandl = false;



    public static void registerHandlers() {
        if (!registredHandl) {
            providers.registerProvider("none", NoProtectHandler.class);
            registredHandl = true;
        }
    }

    public abstract String generateSecureToken(AuthResponse.AuthContext context); //Генерация токена для передачи его в LauncherGuardInterface

    public abstract String generateClientSecureToken();
    public abstract boolean verifyClientSecureToken(String token, String secureKey);
    public abstract boolean allowGetAccessToken(AuthResponse.AuthContext context);

    public abstract void checkLaunchServerLicense(); //Выдает SecurityException при ошибке проверки лицензии
    //public abstract
}
