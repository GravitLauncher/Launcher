package pro.gravit.launcher.events;

import pro.gravit.launcher.request.ResultInterface;

import java.util.UUID;

public abstract class RequestEvent implements ResultInterface {
    public UUID requestUUID;
}
