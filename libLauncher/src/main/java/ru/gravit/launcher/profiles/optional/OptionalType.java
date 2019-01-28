package ru.gravit.launcher.profiles.optional;

import ru.gravit.launcher.LauncherAPI;

@LauncherAPI
public enum OptionalType
{
    @LauncherAPI
    FILE,
    @LauncherAPI
    CLASSPATH,
    @LauncherAPI
    JVMARGS,
    @LauncherAPI
    CLIENTARGS
}
