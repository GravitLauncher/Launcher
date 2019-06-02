package pro.gravit.launcher.profiles.optional;

import pro.gravit.launcher.LauncherAPI;

@LauncherAPI
public enum OptionalType {
    @LauncherAPI
    FILE,
    @LauncherAPI
    CLASSPATH,
    @LauncherAPI
    JVMARGS,
    @LauncherAPI
    CLIENTARGS
}
