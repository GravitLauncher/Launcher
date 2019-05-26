package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.RequestEvent;
import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

public class OAuthConfirmRequestEvent extends RequestEvent implements EventInterface {

    private static final UUID uuid = UUID.fromString("77e1bfd7-adf9-4f5d-87d6-a7dd068deb74");

    @LauncherNetworkAPI
    public String str;

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getType() {
        return "OAuthURL";
    }

    public OAuthConfirmRequestEvent(){
    }
    public OAuthConfirmRequestEvent(String str){
        this.str = str;
    }


}
