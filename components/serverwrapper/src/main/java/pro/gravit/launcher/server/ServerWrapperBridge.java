package pro.gravit.launcher.server;

import pro.gravit.launcher.client.utils.MinecraftAuthlibBridge;
import pro.gravit.utils.helper.LogHelper;

import java.net.InetSocketAddress;

public class ServerWrapperBridge {
    public void run(String host, String port) throws Exception {
        MinecraftAuthlibBridge bridge = new MinecraftAuthlibBridge(new InetSocketAddress(host, Short.parseShort(port)));
        LogHelper.info("Listen %s:%s", host, port);
        Thread.sleep(Long.MAX_VALUE);
    }
}
