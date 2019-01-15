package ru.gravit.launchserver;

@FunctionalInterface
public interface Reloadable {
    void reload() throws Exception;
}
