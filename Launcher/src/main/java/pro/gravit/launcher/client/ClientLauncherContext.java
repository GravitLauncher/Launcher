package pro.gravit.launcher.client;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.PlayerProfile;

public class ClientLauncherContext {
    public Path javaBin;
    public List<String> args = new LinkedList<>();
    public String pathLauncher;
    public ProcessBuilder builder;
    public ClientProfile clientProfile;
    public PlayerProfile playerProfile;
}
