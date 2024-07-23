package pro.gravit.launcher.runtime.gui;

public interface RuntimeProvider {
    void run(String[] args);

    void preLoad();

    void init(boolean clientInstance);
}
