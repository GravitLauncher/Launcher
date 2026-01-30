package pro.gravit.launcher.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.client.utils.MinecraftAuthlibBridge;
import pro.gravit.utils.helper.LogHelper;

import java.net.InetSocketAddress;

public class ServerWrapperBridge {

    private static final Logger logger =
            LoggerFactory.getLogger(ServerWrapperBridge.class);

    public void run(String host, String port) throws Exception {
        MinecraftAuthlibBridge bridge = new MinecraftAuthlibBridge(new InetSocketAddress(host, Short.parseShort(port)));
        logger.info("Listen {}:{}", host, port);
        Thread.sleep(Long.MAX_VALUE);
    }
}