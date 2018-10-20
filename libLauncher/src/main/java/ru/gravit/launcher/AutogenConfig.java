package ru.gravit.launcher;

public class AutogenConfig {
    public String projectname;
    public String address;
    public int port;
    public int clientPort;
    private boolean isInitModules;
    public boolean isUsingWrapper;
    public boolean isDownloadJava; //Выставление этого флага требует модификации runtime части
    public String secretKeyClient;

    AutogenConfig() {

    }

    @SuppressWarnings("UnnecessaryReturnStatement")
    public void initModules() {
        if (isInitModules) return;
    }
}
