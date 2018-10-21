package ru.gravit.launcher.client;

import ru.gravit.launcher.LauncherAPI;

public interface CliParamsInterface {
    @LauncherAPI
    void applySettings();
    @LauncherAPI
    void init(javafx.application.Application.Parameters params);
}
