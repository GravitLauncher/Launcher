package pro.gravit.launcher.profiles.optional;

import pro.gravit.launcher.LauncherNetworkAPI;

@Deprecated
public enum OptionalType {
    @LauncherNetworkAPI
    FILE,
    @LauncherNetworkAPI
    CLASSPATH,
    @LauncherNetworkAPI
    JVMARGS,
    @LauncherNetworkAPI
    CLIENTARGS
}
