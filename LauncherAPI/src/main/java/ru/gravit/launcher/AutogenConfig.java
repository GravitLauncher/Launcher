package ru.gravit.launcher;

public class AutogenConfig {
    public String projectname;
    public String address;
    public int clientPort;
    @SuppressWarnings("unused")
    private boolean isInitModules;
    public String guardType;
    public String secretKeyClient;
    public String guardLicenseName;
    public String guardLicenseKey;
    public String guardLicenseEncryptKey;
    public int env;
    public boolean isWarningMissArchJava;
    // 0 - Dev (дебаг включен по умолчанию, все сообщения)
    // 1 - Debug (дебаг включен по умолчанию, основные сообщения)
    // 2 - Std (дебаг выключен по умолчанию, основные сообщения)
    // 3 - Production (дебаг выключен, минимальный объем сообщений, stacktrace не выводится)

    AutogenConfig() {
    }

    public void initModules() {
    }
}
