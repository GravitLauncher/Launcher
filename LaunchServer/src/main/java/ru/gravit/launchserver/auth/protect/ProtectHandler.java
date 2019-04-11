package ru.gravit.launchserver.auth.protect;

import ru.gravit.launchserver.websocket.json.auth.AuthResponse;
import ru.gravit.utils.helper.VerifyHelper;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ProtectHandler {
    private static final Map<String, Class<? extends ProtectHandler>> PROTECT_HANDLERS = new ConcurrentHashMap<>(4);
    private static boolean registredHandl = false;


    public static void registerHandler(String name, Class<? extends ProtectHandler> adapter) {
        VerifyHelper.verifyIDName(name);
        VerifyHelper.putIfAbsent(PROTECT_HANDLERS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("Protect handler has been already registered: '%s'", name));
    }

    public static Class<? extends ProtectHandler> getHandlerClass(String name) {
        return PROTECT_HANDLERS.get(name);
    }

    public static String getHandlerName(Class<ProtectHandler> clazz) {
        for (Map.Entry<String, Class<? extends ProtectHandler>> e : PROTECT_HANDLERS.entrySet()) {
            if (e.getValue().equals(clazz)) return e.getKey();
        }
        return null;
    }

    public static void registerHandlers() {
        if (!registredHandl) {
            registerHandler("none", NoProtectHandler.class);
            registredHandl = true;
        }
    }

    public abstract String generateSecureToken(AuthResponse.AuthContext context); //Генерация токена для передачи его в LauncherGuardInterface

    public abstract String generateClientSecureToken();
    public abstract boolean verifyClientSecureToken(String token);
    public abstract boolean allowGetAccessToken(AuthResponse.AuthContext context);

    public abstract void checkLaunchServerLicense(); //Выдает SecurityException при ошибке проверки лицензии
    //public abstract
}
