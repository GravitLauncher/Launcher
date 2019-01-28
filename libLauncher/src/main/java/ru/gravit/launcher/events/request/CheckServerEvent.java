package ru.gravit.launcher.events.request;

import ru.gravit.launcher.profiles.PlayerProfile;

import java.util.UUID;

public class CheckServerEvent {
    public String type = "success";
    public String requesttype = "checkServer";
    public UUID uuid;
    public PlayerProfile playerProfile;
}
