package pro.gravit.launchserver.socket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.StdCommandHandler;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisabledOnOs(OS.WINDOWS)
class SocketCommandServerTest {
    @TempDir Path dir;

    @Test
    void acceptsFragmentedUtf8AndCleansUpSocket() throws Exception {
        var result = new CompletableFuture<String>();
        var handler = new StdCommandHandler(false);
        handler.registerCommand("capture", new Command() {
            public String getArgsDescription() { return "value"; }
            public String getUsageDescription() { return "Capture a value"; }
            public void invoke(String... args) { result.complete(args[0]); }
        });
        Path path = dir.resolve("control");
        var server = new SocketCommandServer(handler, path);
        Thread thread = new Thread(server);
        try {
            server.start();
            thread.start();
            try (var channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
                channel.connect(UnixDomainSocketAddress.of(path));
                for (byte value : "capture café\n".getBytes(StandardCharsets.UTF_8)) {
                    var buffer = ByteBuffer.wrap(new byte[]{value});
                    while (buffer.hasRemaining()) channel.write(buffer);
                }
                assertEquals("café", result.get(5, TimeUnit.SECONDS));
            }
        } finally {
            server.close();
            thread.join(5000);
        }
        assertFalse(thread.isAlive());
        assertFalse(Files.exists(path));
    }

    @Test
    void refusesExistingSocketWithoutUnlinkingIt() throws Exception {
        Path path = dir.resolve("control");
        try (var first = new SocketCommandServer(new StdCommandHandler(false), path);
             var second = new SocketCommandServer(new StdCommandHandler(false), path)) {
            first.start();
            assertThrows(IOException.class, second::start);
            assertTrue(Files.exists(path));
        }
    }
}
