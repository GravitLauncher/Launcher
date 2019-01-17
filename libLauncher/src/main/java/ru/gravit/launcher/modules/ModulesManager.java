package ru.gravit.launcher.modules;

import java.net.URL;

public interface ModulesManager extends AutoCloseable {
    void initModules();

    void load(Module module);

    void loadModule(URL jarpath) throws Exception;

    void loadModule(URL jarpath, String classname) throws Exception;

    void postInitModules();

    void preInitModules();

    void finishModules();

    void printModules();

    void sort();

    void registerModule(Module module);
}
