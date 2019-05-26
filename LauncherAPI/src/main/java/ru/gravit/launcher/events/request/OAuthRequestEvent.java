package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.RequestEvent;
import ru.gravit.utils.event.EventInterface;

import java.net.URL;
import java.util.UUID;

public class OAuthRequestEvent extends RequestEvent implements EventInterface {

    private static final UUID uuid = UUID.fromString("77e1bfd7-adf9-4f5d-87d6-a7dd068deb74");

    @LauncherNetworkAPI
    public java.net.URL URL;

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getType() {
        return "oauth";
    }

    public OAuthRequestEvent(){
    }
    public OAuthRequestEvent(URL url){
        this.URL = url;
    }


}
