package ru.gravit.launchserver.response;

import ru.gravit.launcher.request.RequestException;
import ru.gravit.launcher.request.RequestType;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launchserver.LaunchServer;
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
        registerResponse(RequestType.LEGACYLAUNCHER.getNumber(), LegacyLauncherResponse::new);
        registerResponse(RequestType.LAUNCHER.getNumber(), LauncherResponse::new);
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
