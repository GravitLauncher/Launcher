package pro.gravit.launchserver.socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import pro.gravit.launchserver.config.log4j.LogAppender;
import pro.gravit.utils.command.CommandHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclEntryPermission;
import java.util.EnumSet;
import java.util.List;
import java.time.Duration;
import java.util.function.Consumer;

public final class SocketCommandServer implements Runnable, AutoCloseable {
    private static final Logger logger = LogManager.getLogger(SocketCommandServer.class);
    private static final int MAX_COMMAND_BYTES = 64 * 1024;
    private static final Duration IO_TIMEOUT = Duration.ofSeconds(30);
    private final Path path;
    private final CommandHandler commandHandler;
    private ServerSocketChannel serverChannel;
    private volatile SocketChannel clientChannel;
    private volatile boolean closed;
    private boolean bound;

    public SocketCommandServer(CommandHandler commandHandler, Path path) {
        this.commandHandler = commandHandler;
        this.path = path;
    }

    public synchronized void start() throws IOException {
        if (closed || serverChannel != null) throw new IllegalStateException("Control socket cannot be started twice");
        // An existing path may belong to a running instance. Never unlink it here.
        serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        try {
            serverChannel.bind(UnixDomainSocketAddress.of(path));
            bound = true;
            if (Files.getFileAttributeView(path, PosixFileAttributeView.class) != null) {
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
            } else {
                AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
                if (acl == null) throw new IOException("Control socket requires owner-only filesystem permissions");
                acl.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW)
                        .setPrincipal(acl.getOwner()).setPermissions(EnumSet.allOf(AclEntryPermission.class)).build()));
            }
        } catch (IOException | RuntimeException e) {
            close();
            throw e;
        }
    }

    @Override
    public void run() {
        while (!closed) {
            try (SocketChannel channel = serverChannel.accept()) {
                clientChannel = channel;
                if (closed) return;
                channel.configureBlocking(false);
                String command = readCommand(channel);
                if (command != null) runCommand(channel, command);
            } catch (IOException | RuntimeException e) {
                if (!closed) logger.warn("Control socket request failed", e);
            } finally {
                clientChannel = null;
            }
        }
    }

    private static String readCommand(SocketChannel channel) throws IOException {
        ByteArrayOutputStream command = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        long deadline = System.nanoTime() + IO_TIMEOUT.toNanos();
        try (Selector selector = Selector.open()) {
            channel.register(selector, SelectionKey.OP_READ);
            while (true) {
                int count = channel.read(buffer);
                if (count < 0) return null;
                buffer.flip();
                while (buffer.hasRemaining()) {
                    byte value = buffer.get();
                    if (value == '\n') {
                        return StandardCharsets.UTF_8.newDecoder()
                                .onMalformedInput(CodingErrorAction.REPORT)
                                .decode(ByteBuffer.wrap(command.toByteArray())).toString().stripTrailing();
                    }
                    if (command.size() == MAX_COMMAND_BYTES) throw new IOException("Control command exceeds size limit");
                    command.write(value);
                }
                buffer.clear();
                awaitReady(selector, deadline);
            }
        }
    }

    private void runCommand(SocketChannel channel, String command) throws IOException {
        logger.info("Executing control socket command");
        Thread commandThread = Thread.currentThread();
        LogAppender appender = LogAppender.getInstance();
        Consumer<LogEvent> listener = event -> {
            if (Thread.currentThread() != commandThread) return;
            try {
                write(channel, event.getMessage().getFormattedMessage() + "\n");
            } catch (IOException e) {
                // Close the unresponsive peer; do not recursively log through this listener.
                try { channel.close(); } catch (IOException ignored) { }
            }
        };
        if (appender != null) appender.addListener(listener);
        try {
            commandHandler.evalNative(command, false);
        } catch (Exception e) {
            write(channel, "Command failed (" + e.getClass().getSimpleName() + ")\n");
        } finally {
            if (appender != null) appender.removeListener(listener);
        }
    }

    private static void write(SocketChannel channel, String message) throws IOException {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(message);
        long deadline = System.nanoTime() + IO_TIMEOUT.toNanos();
        try (Selector selector = Selector.open()) {
            channel.register(selector, SelectionKey.OP_WRITE);
            while (buffer.hasRemaining()) {
                if (channel.write(buffer) == 0) awaitReady(selector, deadline);
            }
        }
    }

    private static void awaitReady(Selector selector, long deadline) throws IOException {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0 || Thread.currentThread().isInterrupted()) throw new IOException("Control socket I/O timed out");
        selector.select(Math.max(1, Duration.ofNanos(remaining).toMillis()));
        selector.selectedKeys().clear();
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;
        try {
            if (clientChannel != null) clientChannel.close();
        } finally {
            try {
                if (serverChannel != null) serverChannel.close();
            } finally {
                if (bound) Files.deleteIfExists(path);
            }
        }
    }
}
