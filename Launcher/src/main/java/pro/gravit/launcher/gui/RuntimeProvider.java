package pro.gravit.launcher.gui;

public interface RuntimeProvider {
    void run(String[] args) throws Exception;

    void preLoad() throws Exception;

    void init(boolean clientInstance);
}
