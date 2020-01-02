package pro.gravit.launchserver.dao.impl;

import pro.gravit.launcher.hwid.HWID;
import pro.gravit.launcher.hwid.OshiHWID;
import pro.gravit.launchserver.dao.UserHWID;

import javax.persistence.*;
import java.util.function.Supplier;

@Entity
@Table(name = "users_hwids")
public class UserHWIDImpl implements UserHWID {
    private final transient Supplier<OshiHWID> oshiSupp = () -> {
        OshiHWID hwid = new OshiHWID();
        hwid.HWDiskSerial = this.HWDiskSerial;
        hwid.macAddr = this.macAddr;
        hwid.processorID = this.processorID;
        hwid.serialNumber = this.serialNumber;
        hwid.totalMemory = this.totalMemory;
        return hwid;
    };
    private transient OshiHWID oshi = null;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    public final long totalMemory = 0;
    public String serialNumber;
    public String HWDiskSerial;
    public String processorID;
    public String macAddr;

    public boolean banned;

    @Override
    public boolean isBanned() {
        return banned;
    }

    public OshiHWID toHWID() {
        if (oshi == null) oshi = oshiSupp.get();
        return oshi;
    }
}
