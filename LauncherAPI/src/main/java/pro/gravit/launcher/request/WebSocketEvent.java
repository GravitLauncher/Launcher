package pro.gravit.launcher.request;

import pro.gravit.utils.TypeSerializeInterface;

public interface WebSocketEvent extends TypeSerializeInterface {
    String getType();
}
