package pro.gravit.launchserver.legacy;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.RequestType;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.legacy.update.LauncherResponse;
import pro.gravit.launchserver.legacy.update.LegacyLauncherResponse;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.helper.LogHelper;

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


    protected static void writeNoError(HOutput output) throws IOException {
        output.writeString("", 0);
    }
}
