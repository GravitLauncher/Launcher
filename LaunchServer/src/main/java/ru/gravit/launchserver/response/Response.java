package ru.gravit.launchserver.response;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.launcher.request.RequestException;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.auth.AuthResponse;
import ru.gravit.launchserver.response.auth.CheckServerResponse;
import ru.gravit.launchserver.response.auth.JoinServerResponse;
import ru.gravit.launchserver.response.profile.BatchProfileByUsernameResponse;
import ru.gravit.launchserver.response.profile.ProfileByUUIDResponse;
import ru.gravit.launchserver.response.profile.ProfileByUsernameResponse;
import ru.gravit.launchserver.response.update.LauncherResponse;
import ru.gravit.launchserver.response.update.ProfilesResponse;
import ru.gravit.launchserver.response.update.UpdateListResponse;
import ru.gravit.launchserver.response.update.UpdateResponse;

public abstract class Response {
    @FunctionalInterface
    public interface Factory<R> {
        @LauncherAPI
        Response newResponse(LaunchServer server, long id, HInput input, HOutput output, String ip);
    }

    private static final Map<Integer, Factory<?>> RESPONSES = new ConcurrentHashMap<>(8);

    public static Response getResponse(int type, LaunchServer server, long session, HInput input, HOutput output, String ip) {
        return RESPONSES.get(type).newResponse(server, session, input, output, ip);
    }

    public static void registerResponse(int type, Factory<?> factory) {
        RESPONSES.put(type, factory);
    }

    public static void registerResponses() {
        registerResponse(RequestType.PING.getNumber(), PingResponse::new);
        registerResponse(RequestType.AUTH.getNumber(), AuthResponse::new);
        registerResponse(RequestType.CHECK_SERVER.getNumber(), CheckServerResponse::new);
        registerResponse(RequestType.JOIN_SERVER.getNumber(), JoinServerResponse::new);

        registerResponse(RequestType.BATCH_PROFILE_BY_USERNAME.getNumber(), BatchProfileByUsernameResponse::new);
        registerResponse(RequestType.PROFILE_BY_USERNAME.getNumber(), ProfileByUsernameResponse::new);
        registerResponse(RequestType.PROFILE_BY_UUID.getNumber(), ProfileByUUIDResponse::new);

        registerResponse(RequestType.LAUNCHER.getNumber(), LauncherResponse::new);
        registerResponse(RequestType.UPDATE_LIST.getNumber(), UpdateListResponse::new);
        registerResponse(RequestType.UPDATE.getNumber(), UpdateResponse::new);
        registerResponse(RequestType.PROFILES.getNumber(), ProfilesResponse::new);
    }

    @LauncherAPI
    public static void requestError(String message) throws RequestException {
        throw new RequestException(message);
    }

    @LauncherAPI
    protected final LaunchServer server;

    @LauncherAPI
    protected final HInput input;

    @LauncherAPI
    protected final HOutput output;

    @LauncherAPI
    protected final String ip;

    @LauncherAPI
    protected final long session;

    protected Response(LaunchServer server, long session, HInput input, HOutput output, String ip) {
        this.server = server;
        this.input = input;
        this.output = output;
        this.ip = ip;
        this.session = session;
    }

    @LauncherAPI
    protected final void debug(String message) {
        LogHelper.subDebug("#%d %s", session, message);
    }

    @LauncherAPI
    protected final void debug(String message, Object... args) {
        debug(String.format(message, args));
    }

    @LauncherAPI
    public abstract void reply() throws Exception;

    @LauncherAPI
    @SuppressWarnings("MethodMayBeStatic") // Intentionally not static
    protected final void writeNoError(HOutput output) throws IOException {
        output.writeString("", 0);
    }
}
