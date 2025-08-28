package pro.gravit.launcher.client;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileVersions;
import pro.gravit.launcher.base.profiles.PlayerProfile;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalActionClientArgs;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.utils.Version;

import java.util.*;

public class ClientParams {
    public long timestamp;
    public String assetDir;

    public String clientDir;

    public String resourcePackDir;

    public String nativesDir;

    // Client params

    public PlayerProfile playerProfile;

    public ClientProfile profile;

    public String accessToken;

    //==Minecraft params==

    public boolean autoEnter;

    public boolean fullScreen;
    public boolean lwjglGlfwWayland;

    public int ram;

    public int width;

    public int height;

    public Set<OptionalAction> actions = new HashSet<>();

    //========

    public UUID session;

    public AuthRequestEvent.OAuthRequestEvent oauth;

    public String authId;

    public long oauthExpiredTime;

    public Map<String, Request.ExtendedToken> extendedTokens;

    public boolean offlineMode;

    public transient HashedDir assetHDir;

    public transient HashedDir clientHDir;

    public transient HashedDir javaHDir;

    public void addClientArgs(Collection<String> args) {
        if (profile.getVersion().compareTo(ClientProfileVersions.MINECRAFT_1_6_4) >= 0)
            addModernClientArgs(args);
        else
            addClientLegacyArgs(args);
    }

    public void addClientLegacyArgs(Collection<String> args) {
        args.add(playerProfile.username);
        args.add(accessToken);

        // Add args for tweaker
        Collections.addAll(args, "--version", profile.getVersion().toString());
        Collections.addAll(args, "--gameDir", clientDir);
        Collections.addAll(args, "--assetsDir", assetDir);
    }

    private void addModernClientArgs(Collection<String> args) {

        // Add version-dependent args
        ClientProfile.Version version = profile.getVersion();
        Collections.addAll(args, "--username", playerProfile.username);
        if (version.compareTo(ClientProfileVersions.MINECRAFT_1_7_2) >= 0) {
            Collections.addAll(args, "--uuid", Launcher.toHash(playerProfile.uuid));
            Collections.addAll(args, "--accessToken", accessToken);

            // Add 1.7.10+ args (user properties, asset index)
            if (version.compareTo(ClientProfileVersions.MINECRAFT_1_7_10) >= 0) {
                // Add user properties
                Collections.addAll(args, "--userType", "mojang");
                Collections.addAll(args, "--userProperties", "{}");

                // Add asset index
                Collections.addAll(args, "--assetIndex", profile.getAssetIndex());
            }
        } else
            Collections.addAll(args, "--session", accessToken);

        // Add version and dirs args
        Collections.addAll(args, "--version", profile.getVersion().toString());
        Collections.addAll(args, "--gameDir", clientDir);
        Collections.addAll(args, "--assetsDir", assetDir);
        Collections.addAll(args, "--resourcePackDir", resourcePackDir);
        if (version.compareTo(ClientProfileVersions.MINECRAFT_1_9_4) >= 0)
            Collections.addAll(args, "--versionType", "Launcher v" + Version.getVersion().getVersionString());

        // Add server args
        if (autoEnter) {
            if (version.compareTo(ClientProfileVersions.MINECRAFT_1_20) <= 0) {
                Collections.addAll(args, "--server", profile.getServerAddress());
                Collections.addAll(args, "--port", Integer.toString(profile.getServerPort()));
            } else {
                Collections.addAll(args, "--quickPlayMultiplayer", String.format("%s:%d", profile.getServerAddress(), profile.getServerPort()));
            }
        }
        for (OptionalAction a : actions) {
            if (a instanceof OptionalActionClientArgs) {
                args.addAll(((OptionalActionClientArgs) a).args);
            }
        }
        // Add window size args
        if (fullScreen)
            Collections.addAll(args, "--fullscreen", Boolean.toString(true));
        if (width > 0 && height > 0) {
            Collections.addAll(args, "--width", Integer.toString(width));
            Collections.addAll(args, "--height", Integer.toString(height));
        }
    }
}
