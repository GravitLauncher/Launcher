package ru.gravit.launcher.modules;

import java.net.URL;

public interface ModulesManagerInterface {
    void initModules();

    void load(Module module);

    void loadModule(URL jarpath) throws Exception;

    void loadModule(URL jarpath, String classname) throws Exception;

    void postInitModules();

    void preInitModules();

    void printModules();

    void sort();

    void registerModule(Module module, boolean preload); // hacky method for client
}
