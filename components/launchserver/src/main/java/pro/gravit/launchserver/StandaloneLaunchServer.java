package pro.gravit.launchserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.socket.SocketCommandServer;
import pro.gravit.utils.command.JLineCommandHandler;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns process resources for the standalone entry point. */
public final class StandaloneLaunchServer implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(StandaloneLaunchServer.class);
    private final LaunchServer server;
    private final SocketCommandServer socket;
    private final Thread shutdownHook;
    private final AtomicBoolean closed = new AtomicBoolean();
    private Thread consoleThread;
    private Thread socketThread;

    public StandaloneLaunchServer(LaunchServer server) {
        this.server = server;
        socket = new SocketCommandServer(server.commandHandler, server.controlFile);
        shutdownHook = new Thread(this::closeQuietly, "LaunchServer shutdown");
    }

    public void run() throws Exception {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            socket.start();
            server.start();
            consoleThread = new Thread(() -> {
                try { server.commandHandler.run(); } finally { closeQuietly(); }
            }, "Command Thread");
            consoleThread.setDaemon(true);
            socketThread = new Thread(socket, "Socket Command Thread");
            socketThread.setDaemon(true);
            socketThread.start();
            consoleThread.start();
            server.awaitTermination();
        } finally {
            close();
        }
    }

    private void closeQuietly() {
        try { close(); } catch (Exception e) { logger.error("LaunchServer shutdown failed", e); }
    }

    @Override
    public void close() throws Exception {
        if (!closed.compareAndSet(false, true)) return;
        server.commandHandler.requestStop();
        Exception failure = null;
        try { socket.close(); } catch (IOException e) { failure = e; }
        try { server.close(); } catch (Exception e) {
            if (failure == null) failure = e; else failure.addSuppressed(e);
        }
        if (consoleThread != null && Thread.currentThread() != consoleThread) consoleThread.interrupt();
        if (socketThread != null && Thread.currentThread() != socketThread) socketThread.interrupt();
        if (server.commandHandler instanceof JLineCommandHandler terminal) {
            try { terminal.closeTerminal(); } catch (IOException e) {
                if (failure == null) failure = e; else failure.addSuppressed(e);
            }
        }
        if (Thread.currentThread() != shutdownHook) {
            try { Runtime.getRuntime().removeShutdownHook(shutdownHook); } catch (IllegalStateException ignored) { }
        }
        if (failure != null) throw failure;
    }
}
