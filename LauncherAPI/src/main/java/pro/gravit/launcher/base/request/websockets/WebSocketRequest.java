package pro.gravit.launcher.base.request.websockets;

import pro.gravit.utils.TypeSerializeInterface;

public interface WebSocketRequest extends TypeSerializeInterface {
    String getType();
}
