package ru.gravit.utils.websocket;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

import jdk.nashorn.internal.ir.RuntimeNode;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.utils.helper.IOHelper;

/**
 * Basic Echo Client Socket
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
public class WebSocketClient
{
    private final CountDownLatch closeLatch;
    @SuppressWarnings("unused")
    private Session session;
    private final MessageInterface adapter;
    public  WebSocketClient(MessageInterface adapter)
    {
        this.closeLatch = new CountDownLatch(1);
        this.adapter = adapter;
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException
    {
        return this.closeLatch.await(duration,unit);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason)
    {
        System.out.printf("Connection closed: %d - %s%n",statusCode,reason);
        this.session = null;
        this.closeLatch.countDown(); // trigger latch
    }

    @OnWebSocketConnect
    public void onConnect(Session session)
    {
        System.out.printf("Got connect: %s%n",session);
        this.session = session;
    }
    public void request(ByteBuffer buffer) throws IOException {
        session.getRemote().sendBytes(buffer);
    }
    @OnWebSocketMessage
    public void onMessage(String msg) throws IOException {
        byte[] bytes = msg.getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);
        HInput input = new HInput(stream);
        adapter.request(input);
    }
}
