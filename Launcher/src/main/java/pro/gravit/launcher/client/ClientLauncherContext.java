package pro.gravit.launcher.client;

import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.PlayerProfile;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class ClientLauncherContext {
    public Path javaBin;
    public final List<String> args = new LinkedList<>();
    public String pathLauncher;
    public ProcessBuilder builder;
    public Process process;
    public ClientProfile clientProfile;
    public PlayerProfile playerProfile;
    public ClientLauncher.Params params;
}
