package ru.gravit.launchserver;

public interface Reconfigurable {
    void reconfig(String action, String[] args);

    void printConfigHelp();
}
