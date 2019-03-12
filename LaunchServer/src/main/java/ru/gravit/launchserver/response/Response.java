package ru.gravit.launchserver.response;

import ru.gravit.launcher.request.RequestException;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.response.admin.ExecCommandResponse;
import ru.gravit.launchserver.response.auth.*;
import ru.gravit.launchserver.response.profile.BatchProfileByUsernameResponse;
import ru.gravit.launchserver.response.profile.ProfileByUUIDResponse;
import ru.gravit.launchserver.response.profile.ProfileByUsernameResponse;
import ru.gravit.launchserver.response.update.*;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Response {
    @FunctionalInterface
    public interface Factory<R> {

        Response newResponse(LaunchServer server, long id, HInput input, HOutput output, String ip, Client clientData);
    }

    private static final Map<Integer, Factory<?>> RESPONSES = new ConcurrentHashMap<>(8);

    public static Response getResponse(int type, LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        return RESPONSES.get(type).newResponse(server, session, input, output, ip, clientData);
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

        registerResponse(RequestType.LEGACYLAUNCHER.getNumber(), LegacyLauncherResponse::new);
        registerResponse(RequestType.LAUNCHER.getNumber(), LauncherResponse::new);
        registerResponse(RequestType.UPDATE_LIST.getNumber(), UpdateListResponse::new);
        registerResponse(RequestType.UPDATE.getNumber(), UpdateResponse::new);
        registerResponse(RequestType.PROFILES.getNumber(), ProfilesResponse::new);
        registerResponse(RequestType.SERVERAUTH.getNumber(), AuthServerResponse::new);
        registerResponse(RequestType.SETPROFILE.getNumber(), SetProfileResponse::new);
        registerResponse(RequestType.CHANGESERVER.getNumber(), ChangeServerResponse::new);
        registerResponse(RequestType.EXECCOMMAND.getNumber(), ExecCommandResponse::new);
    }


    public static void requestError(String message) throws RequestException {
        throw new RequestException(message);
    }


    protected final LaunchServer server;


    protected final HInput input;


    protected final HOutput output;


    protected final String ip;

    protected final Client clientData;


    protected final long session;

    protected Response(LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        this.server = server;
        this.input = input;
        this.output = output;
        this.ip = ip;
        this.session = session;
        this.clientData = clientData;
    }


    protected final void debug(String message) {
        LogHelper.subDebug("#%d %s", session, message);
    }


    protected final void debug(String message, Object... args) {
        debug(String.format(message, args));
    }


    public abstract void reply() throws Exception;


    protected static final void writeNoError(HOutput output) throws IOException {
        output.writeString("", 0);
    }
}
