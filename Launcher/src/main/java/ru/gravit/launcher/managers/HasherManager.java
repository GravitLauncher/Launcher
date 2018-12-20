package ru.gravit.launcher.managers;

public class HasherManager {
    public static final HasherStore defaultStore = new HasherStore();

    public static HasherStore getDefaultStore() {
        return defaultStore;
    }
}
