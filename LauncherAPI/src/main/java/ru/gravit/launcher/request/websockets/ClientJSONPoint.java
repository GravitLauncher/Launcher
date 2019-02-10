package ru.gravit.launcher.request.websockets;

import java.net.URI;
import java.util.Map;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import ru.gravit.utils.helper.LogHelper;

public class ClientJSONPoint extends WebSocketClient {

	public ClientJSONPoint(URI serverUri, Map<String, String> httpHeaders, int connectTimeout) {
		super(serverUri, new Draft_6455(), httpHeaders, connectTimeout);
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		
	}

	@Override
	public void onMessage(String message) {
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		LogHelper.debug("Disconnected: " + code + " " + remote + " " + reason != null ? reason : "no reason");
	}

	@Override
	public void onError(Exception ex) {
		LogHelper.error(ex);
	}

}