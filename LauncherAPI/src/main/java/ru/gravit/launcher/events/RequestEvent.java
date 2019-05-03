package ru.gravit.launcher.events;

import ru.gravit.launcher.request.ResultInterface;

import java.util.UUID;

public abstract class RequestEvent implements ResultInterface {
    public UUID requestUUID;
}
