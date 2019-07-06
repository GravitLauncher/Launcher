package pro.gravit.launcher.request.websockets;

import pro.gravit.utils.TypeSerializeInterface;

public interface WebSocketRequest extends TypeSerializeInterface {
    String getType();
}
