package pro.gravit.launcher.events;

import java.util.UUID;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.request.WebSocketEvent;

/**
 * The class of all request events sent by the server to the client
 */
public abstract class RequestEvent implements WebSocketEvent {
    /**
     * UUID sent in request
     */
    @LauncherNetworkAPI
    public UUID requestUUID;
}
