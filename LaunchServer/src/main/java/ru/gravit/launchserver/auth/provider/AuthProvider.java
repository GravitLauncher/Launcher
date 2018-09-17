package ru.gravit.launchserver.auth.provider;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.helper.VerifyHelper;
import ru.gravit.launcher.serialize.config.ConfigObject;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launchserver.auth.AuthException;

public abstract class AuthProvider extends ConfigObject implements AutoCloseable {
    private static final Map<String, Adapter<AuthProvider>> AUTH_PROVIDERS = new ConcurrentHashMap<>(8);
    private static boolean registredProv = false;

    @LauncherAPI
    public static AuthProviderResult authError(String message) throws AuthException {
        throw new AuthException(message);
    }

    @LauncherAPI
    public static AuthProvider newProvider(String name, BlockConfigEntry block) {
        VerifyHelper.verifyIDName(name);
        Adapter<AuthProvider> authHandlerAdapter = VerifyHelper.getMapValue(AUTH_PROVIDERS, name,
                String.format("Unknown auth provider: '%s'", name));
        return authHandlerAdapter.convert(block);
    }

    @LauncherAPI
    public static void registerProvider(String name, Adapter<AuthProvider> adapter) {
        VerifyHelper.putIfAbsent(AUTH_PROVIDERS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("Auth provider has been already registered: '%s'", name));
    }

    public static void registerProviders() {
        if (!registredProv) {
            registerProvider("null", NullAuthProvider::new);
            registerProvider("accept", AcceptAuthProvider::new);
            registerProvider("reject", RejectAuthProvider::new);

            // Auth providers that doesn't do nothing :D
            registerProvider("mojang", MojangAuthProvider::new);
            registerProvider("mysql", MySQLAuthProvider::new);
            registerProvider("file", FileAuthProvider::new);
            registerProvider("request", RequestAuthProvider::new);
            registerProvider("json", JsonAuthProvider::new);
            registredProv = true;
        }
    }

    @LauncherAPI
    protected AuthProvider(BlockConfigEntry block) {
        super(block);
    }

    @LauncherAPI
    public abstract AuthProviderResult auth(String login, String password, String ip) throws Exception;

    @Override
    public abstract void close() throws IOException;
}
