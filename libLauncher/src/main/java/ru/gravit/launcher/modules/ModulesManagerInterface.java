package ru.gravit.launcher.modules;

import java.net.URL;

public interface ModulesManagerInterface {
    void initModules();

    void load(Module module);

    void load(Module module, boolean preload);

    void loadModule(URL jarpath, boolean preload) throws Exception;

    void loadModule(URL jarpath, String classname, boolean preload) throws Exception;

    void postInitModules();

    void preInitModules();

    void printModules();

    void sort();

    void registerModule(Module module, boolean preload);
}
