package pro.gravit.launcher.events;

import java.util.UUID;

import pro.gravit.launcher.request.ResultInterface;

public abstract class RequestEvent implements ResultInterface {
    public UUID requestUUID;
}
