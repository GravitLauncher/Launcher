package pro.gravit.launcher.gui;

public interface RuntimeProvider {
    void run(String[] args);

    void preLoad();

    void init(boolean clientInstance);
}
