package ru.gravit.launcher.request.websockets;

import java.io.IOException;
import java.io.Reader;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import ru.gravit.utils.helper.LogHelper;

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
		try {
			JsonValue json = Json.parse(message);
		} catch (ParseException | IOException ex) {
			LogHelper.error(ex);
		}
	}
	
	public void send(JsonValue js) throws IOException {
		session.getBasicRemote().sendText(js.asString());
	}
	
	public void sendAsync(JsonValue js) throws IOException {
		session.getAsyncRemote().sendText(js.asString());
	}
}