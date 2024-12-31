package pro.gravit.launchserver.socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.config.log4j.LogAppender;
import pro.gravit.utils.command.CommandHandler;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SocketCommandServer implements Runnable {
    private final Logger logger = LogManager.getLogger(SocketCommandServer.class);
    private ServerSocketChannel channel;
    private Path path;
    private UnixDomainSocketAddress address;
    private ServerSocketChannel serverChannel;
    private CommandHandler commandHandler;
    private transient SocketChannel clientChannel;

    public SocketCommandServer(CommandHandler commandHandler, Path path) {
        this.commandHandler = commandHandler;
        this.path = path;
    }

    private void runCommand(SocketChannel channel, String command) {
        logger.info("Command '{}' from socket", command);
        clientChannel = channel;
        try {
            commandHandler.evalNative(command, false);
        } catch (Throwable e) {
            logger.error("Error when execute command", e);
        } finally {
            clientChannel = null;

        }
    }

    @Override
    public void run() {
        try {
            Files.deleteIfExists(path);
            this.address = UnixDomainSocketAddress.of(path);
            serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            serverChannel.configureBlocking(true);
            serverChannel.bind(address);
            LogAppender.getInstance().addListener((logEvent -> {
                if(clientChannel != null && clientChannel.isOpen()) {
                    try {
                        String s = logEvent.getMessage().getFormattedMessage()+"\n";
                        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                        ByteBuffer buffer = ByteBuffer.wrap(bytes);
                        clientChannel.write(buffer);
                    } catch (Throwable ignored) {
                    }
                }
            }));
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (true) {
                SocketChannel channel = serverChannel.accept();
                channel.configureBlocking(true);
                String command = null;
                try {
                    mark:
                    while (true) {
                        int bytesRead = channel.read(buffer);
                        if (bytesRead < 0) {
                            break;
                        }
                        for (var i=0;i<buffer.limit();i++) {
                            if(buffer.get(i) == '\n') {
                                command = new String(buffer.array(), 0, i);
                                break mark;
                            }
                        }

                    }
                    if(command != null) {
                        runCommand(channel, command);
                    }
                } finally {
                    buffer.clear();
                    channel.close();
                }
            }
        } catch (Throwable e) {
            logger.error("Unix command socket server error", e);
        }
    }
}
