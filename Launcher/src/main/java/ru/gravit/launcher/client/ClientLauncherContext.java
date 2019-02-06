package ru.gravit.launcher.client;

import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.profiles.PlayerProfile;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class ClientLauncherContext {
    public Path javaBin;
    public List<String> args = new LinkedList<>();
    public String pathLauncher;
    public ProcessBuilder builder;
    public ClientProfile clientProfile;
    public PlayerProfile playerProfile;
}
