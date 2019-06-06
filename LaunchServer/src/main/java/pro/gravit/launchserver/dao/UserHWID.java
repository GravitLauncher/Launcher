package pro.gravit.launchserver.dao;

import pro.gravit.launcher.LauncherAPI;

import javax.persistence.*;

@Entity
@Table(name = "users_hwids")
public class UserHWID {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @LauncherAPI
    public long totalMemory = 0;
    @LauncherAPI
    public String serialNumber;
    @LauncherAPI
    public String HWDiskSerial;
    @LauncherAPI
    public String processorID;
    @LauncherAPI
    public String macAddr;
}
