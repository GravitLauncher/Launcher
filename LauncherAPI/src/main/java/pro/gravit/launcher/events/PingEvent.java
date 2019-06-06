package pro.gravit.launcher.events;

import java.util.UUID;

import pro.gravit.utils.event.EventInterface;

//Пустое событие
//Все обработчики обязаны его игнорировать
public final class PingEvent implements EventInterface {
    private static final UUID uuid = UUID.fromString("7c8be7e7-82ce-4c99-84cd-ee8fcce1b509");

    @Override
    public UUID getUUID() {

        return uuid;
    }
}
