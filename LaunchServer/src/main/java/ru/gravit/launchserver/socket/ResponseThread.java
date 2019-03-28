package ru.gravit.launchserver.socket;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.request.RequestException;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.manangers.SessionManager;
import ru.gravit.launchserver.manangers.hook.SocketHookManager;
import ru.gravit.launchserver.response.Response;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;

public final class ResponseThread implements Runnable {
    class Handshake {
        int type;
        long session;

        public Handshake(int type, long session) {
            this.type = type;
            this.session = session;
        }
    }

    private final LaunchServer server;
    private final Socket socket;

    private final SessionManager sessions;
    private final SocketHookManager socketHookManager;

    public ResponseThread(LaunchServer server, long id, Socket socket, SessionManager sessionManager, SocketHookManager socketHookManager) throws SocketException {
        this.server = server;
        this.socket = socket;
        sessions = sessionManager;
        this.socketHookManager = socketHookManager;
        // Fix socket flags
        IOHelper.setSocketFlags(socket);
    }

    private Handshake readHandshake(HInput input, HOutput output) throws IOException {
        boolean legacy = false;
        long session = 0;
        // Verify magic number
        int magicNumber = input.readInt();
        if (magicNumber != Launcher.PROTOCOL_MAGIC)
            if (magicNumber == Launcher.PROTOCOL_MAGIC_LEGACY - 1) { // Previous launcher protocol
                session = 0;
                legacy = true;
            } else if (magicNumber == ServerSocketHandler.LEGACY_LAUNCHER_MAGIC) { // Previous launcher protocol
                session = 0;
                legacy = true;
            } else if (magicNumber == Launcher.PROTOCOL_MAGIC_LEGACY) {

            } else
                throw new IOException("Invalid Handshake");
        // Verify key modulus
        BigInteger keyModulus = input.readBigInteger(SecurityHelper.RSA_KEY_LENGTH + 1);
        if (!legacy) {
            session = input.readLong();
            sessions.updateClient(session);
        }
        if (!keyModulus.equals(server.privateKey.getModulus())) {
            output.writeBoolean(false);
            throw new IOException(String.format("#%d Key modulus mismatch", session));
        }
        // Read request type
        int type = input.readVarInt();
        if (!server.serverSocketHandler.onHandshake(session, type)) {
            output.writeBoolean(false);
            return null;
        }

        // Protocol successfully verified
        output.writeBoolean(true);
        output.flush();
        return new Handshake(type, session);
    }

    private void respond(Integer type, HInput input, HOutput output, long session, String ip, Client clientData) throws Exception {
        if (server.serverSocketHandler.logConnections)
            LogHelper.info("Connection #%d from %s", session, ip);

        // Choose response based on type
        Response response = Response.getResponse(type, server, session, input, output, ip, clientData);

        // Reply
        response.reply();
        LogHelper.subDebug("#%d Replied", session);
    }

    @Override
    public void run() {
        if (!server.serverSocketHandler.logConnections)
            LogHelper.debug("Connection from %s", IOHelper.getIP(socket.getRemoteSocketAddress()));

        // Process connection
        boolean cancelled = false;
        Exception savedError = null;
        try (HInput input = new HInput(socket.getInputStream());
             HOutput output = new HOutput(socket.getOutputStream())) {
            Handshake handshake = readHandshake(input, output);
            if (handshake == null) { // Not accepted
                cancelled = true;
                return;
            }
            SocketContext context = new SocketContext();
            context.input = input;
            context.output = output;
            context.ip = IOHelper.getIP(socket.getRemoteSocketAddress());
            context.session = handshake.session;
            context.type = handshake.type;
            Client clientData = server.sessionManager.getOrNewClient(context.session);
            context.client = clientData;

            // Start response
            if (socketHookManager.preHook(context)) {
                try {
                    respond(handshake.type, input, output, handshake.session, context.ip, clientData);
                    socketHookManager.postHook(context);
                } catch (RequestException e) {
                    if (server.socketHookManager.errorHook(context, e)) {
                        LogHelper.subDebug(String.format("#%d Request error: %s", handshake.session, e.getMessage()));
                        if (e.getMessage() == null) LogHelper.error(e);
                        output.writeString(e.getMessage(), 0);
                    }
                }
            }
        } catch (Exception e) {
            savedError = e;
            if (server.socketHookManager.fatalErrorHook(socket, e))
                LogHelper.error(e);
        } finally {
            IOHelper.close(socket);
            if (!cancelled)
                server.serverSocketHandler.onDisconnect(savedError);
        }
    }
}
