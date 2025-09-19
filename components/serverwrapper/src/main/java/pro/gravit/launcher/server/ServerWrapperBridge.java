package pro.gravit.launcher.server;

import pro.gravit.launcher.client.utils.MinecraftAuthlibBridge;

import java.net.InetSocketAddress;

public class ServerWrapperBridge {
    public void run(String host, String port) throws Exception {
        MinecraftAuthlibBridge bridge = new MinecraftAuthlibBridge(new InetSocketAddress(host, Short.parseShort(port)));
        Thread.sleep(Long.MAX_VALUE);
    }
}
