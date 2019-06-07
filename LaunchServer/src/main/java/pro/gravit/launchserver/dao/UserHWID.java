package pro.gravit.launchserver.dao;

import pro.gravit.launcher.HWID;
import pro.gravit.launcher.OshiHWID;

import javax.persistence.*;
import java.util.function.Supplier;

@Entity
@Table(name = "users_hwids")
public class UserHWID implements HWID {
    private transient Supplier<OshiHWID> oshiSupp = () -> {
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

    public long totalMemory = 0;
    public String serialNumber;
    public String HWDiskSerial;
    public String processorID;
    public String macAddr;

    public boolean banned;
    public OshiHWID toHWID()
    {
        if(oshi == null) oshi = oshiSupp.get();
        return oshi;
    }

    @Override
    public String getSerializeString() {
        return toHWID().getSerializeString();
    }

    @Override
    public int getLevel() {
        return toHWID().getLevel();
    }

    @Override
    public int compare(HWID hwid) {
        return toHWID().compare(hwid);
    }

    @Override
    public boolean isNull() {
        return toHWID().isNull();
    }
}
