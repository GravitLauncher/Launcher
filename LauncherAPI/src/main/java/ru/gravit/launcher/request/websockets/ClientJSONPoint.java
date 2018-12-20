package ru.gravit.launcher.request.websockets;

import javax.websocket.*;
import java.io.IOException;
import java.io.Reader;

/*
 * public class Client {
 *
 *   final static CountDownLatch messageLatch = new CountDownLatch(1);
 *
 *   public static void main(String[] args) {
 *       try {
 *           WebSocketContainer container = ContainerProvider.getWebSocketContainer();
 *           String uri = "ws://echo.websocket.org:80/";
 *           System.out.println("Connecting to " + uri);
 *           container.connectToServer(MyClientEndpoint.class, URI.create(uri));
 *           messageLatch.await(100, TimeUnit.SECONDS);
 *       } catch (DeploymentException | InterruptedException | IOException ex) {
 *           Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
 *       }
 *   }
 * }
 */
@ClientEndpoint
public class ClientJSONPoint {
    public Session session = null;
    private ClientWebSocketService service;

    public void setService(ClientWebSocketService service) {
        this.service = service;
    }

    @OnOpen
    public void onOpen(final Session session_r) {
        session = session_r;
        System.out.println("Connected to endpoint: " + session.getBasicRemote());
    }

    @OnError
    public void processError(final Throwable t) {
        t.printStackTrace();
    }

    @OnMessage
    public void processMessage(Reader message) {
        service.processMessage(message);
    }

    public void send(String js) throws IOException {
        session.getBasicRemote().sendText(js);
    }

    public void sendAsync(String js) throws IOException {
        session.getAsyncRemote().sendText(js);
    }
}