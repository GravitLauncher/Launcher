package ru.gravit.launcher.modules;

import java.net.URL;

public interface ModulesManagerInterface {
    void initModules() throws Exception;
    void load(Module module) throws Exception;
    void load(Module module, boolean preload) throws Exception;
    void loadModule(URL jarpath, boolean preload) throws Exception;
    void loadModule(URL jarpath, String classname, boolean preload) throws Exception;
    void postInitModules() throws Exception;
    void preInitModules() throws Exception;
    void printModules() throws Exception;
    void registerModule(Module module,boolean preload) throws Exception;
}
