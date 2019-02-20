package ru.gravit.launcher.request;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Request<R> {
    private static long session = SecurityHelper.secureRandom.nextLong();

    public static void setSession(long session) {
        Request.session = session;
    }

    public static long getSession() {
        return Request.session;
    }

    @LauncherAPI
    public static void requestError(String message) throws RequestException {
        throw new RequestException(message);
    }

    @LauncherAPI
    protected transient final LauncherConfig config;

    private transient final AtomicBoolean started = new AtomicBoolean(false);

    @LauncherAPI
    protected Request() {
        this(null);
    }

    @LauncherAPI
    protected Request(LauncherConfig config) {
        this.config = config == null ? Launcher.getConfig() : config;
    }

    @LauncherAPI
    public abstract Integer getLegacyType();

    @LauncherAPI
    protected final void readError(HInput input) throws IOException {
        String error = input.readString(0);
        if (!error.isEmpty())
            requestError(error);
    }

    @LauncherAPI
    public R request() throws Exception {
        if (!started.compareAndSet(false, true))
            throw new IllegalStateException("Request already started");
        R wsResult = null;
        if(config.isNettyEnabled)
            wsResult = requestWebSockets();
        if(wsResult != null) return wsResult;
        // Make request to LaunchServer
        try (Socket socket = IOHelper.newSocket()) {
            socket.connect(IOHelper.resolve(config.address));
            try (HInput input = new HInput(socket.getInputStream());
                 HOutput output = new HOutput(socket.getOutputStream())) {
                writeHandshake(input, output);
                return requestDo(input, output);
            }
        }
    }
    protected R requestWebSockets() throws Exception
    {
        return null;
    }
    @LauncherAPI
    protected abstract R requestDo(HInput input, HOutput output) throws Exception;

    private void writeHandshake(HInput input, HOutput output) throws IOException {
        // Write handshake
        output.writeInt(Launcher.PROTOCOL_MAGIC);
        output.writeBigInteger(config.publicKey.getModulus(), SecurityHelper.RSA_KEY_LENGTH + 1);
        output.writeLong(session);
        output.writeVarInt(getLegacyType());
        output.flush();

        // Verify is accepted
        if (!input.readBoolean())
            requestError("Serverside not accepted this connection");
    }

}
