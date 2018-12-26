package ru.gravit.launcher;

public class AutogenConfig {
    public String projectname;
    public String address;
    public int port;
    public int clientPort;
    private boolean isInitModules = false;
    public boolean isUsingWrapper;
    public boolean isDownloadJava; //Выставление этого флага требует модификации runtime части
    public String secretKeyClient;
    public int env;
    public boolean isWarningMissArchJava;
    // 0 - Dev (дебаг включен по умолчанию, все сообщения)
    // 1 - Debug (дебаг включен по умолчанию, основные сообщения)
    // 2 - Std (дебаг выключен по умолчанию, основные сообщения)
    // 3 - Production (дебаг выключен, минимальный объем сообщений, stacktrace не выводится)

    AutogenConfig() {

    }

    @SuppressWarnings("UnnecessaryReturnStatement")
    public void initModules() {
        if (isInitModules) return;
    }
}
