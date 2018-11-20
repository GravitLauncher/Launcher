package ru.gravit.launchserver.auth.handler;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.serialize.config.ConfigObject;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;

public abstract class AuthHandler extends ConfigObject implements AutoCloseable {
    private static final Map<String, Adapter<AuthHandler>> AUTH_HANDLERS = new ConcurrentHashMap<>(4);
    private static boolean registredHandl = false;


    public static UUID authError(String message) throws AuthException {
        throw new AuthException(message);
    }


    public static AuthHandler newHandler(String name, BlockConfigEntry block) {
        Adapter<AuthHandler> authHandlerAdapter = VerifyHelper.getMapValue(AUTH_HANDLERS, name,
                String.format("Unknown auth handler: '%s'", name));
        return authHandlerAdapter.convert(block);
    }


    public static void registerHandler(String name, Adapter<AuthHandler> adapter) {
        VerifyHelper.verifyIDName(name);
        VerifyHelper.putIfAbsent(AUTH_HANDLERS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("Auth handler has been already registered: '%s'", name));
    }

    public static void registerHandlers() {
        if (!registredHandl) {
            registerHandler("null", NullAuthHandler::new);
            registerHandler("memory", MemoryAuthHandler::new);

            // Auth handler that doesn't do nothing :D
            registerHandler("binaryFile", BinaryFileAuthHandler::new);
            registerHandler("textFile", TextFileAuthHandler::new);
            registerHandler("mysql", MySQLAuthHandler::new);
            registerHandler("json", JsonAuthHandler::new);
            registredHandl = true;
        }
    }


    protected AuthHandler(BlockConfigEntry block) {
        super(block);
    }

    public abstract UUID auth(AuthProviderResult authResult) throws IOException;

    public abstract UUID checkServer(String username, String serverID) throws IOException;

    @Override
    public abstract void close() throws IOException;


    public abstract boolean joinServer(String username, String accessToken, String serverID) throws IOException;


    public abstract UUID usernameToUUID(String username) throws IOException;


    public abstract String uuidToUsername(UUID uuid) throws IOException;
}
