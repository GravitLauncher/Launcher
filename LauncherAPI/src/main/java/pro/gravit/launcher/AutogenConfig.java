package pro.gravit.launcher;

public class AutogenConfig {
    public String projectname;
    public String address;
    public int clientPort;
    public String guardType;
    public String secretKeyClient;
    public String oemUnlockKey;
    public String guardLicenseName;
    public String guardLicenseKey;
    public String guardLicenseEncryptKey;
    public String secureCheckHash;
    public String secureCheckSalt;
    public String passwordEncryptKey;
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
