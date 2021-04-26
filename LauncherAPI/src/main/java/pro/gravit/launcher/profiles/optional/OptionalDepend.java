package pro.gravit.launcher.profiles.optional;

import pro.gravit.launcher.LauncherNetworkAPI;

public class OptionalDepend {
    @LauncherNetworkAPI
    public String name;
    @Deprecated
    @LauncherNetworkAPI
    public OptionalType type;
}
