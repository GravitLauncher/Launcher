package ru.gravit.launcher.hwid;

import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import ru.gravit.launcher.HWID;
import ru.gravit.launcher.LauncherHWIDInterface;
import ru.gravit.launcher.OshiHWID;
import ru.gravit.utils.helper.LogHelper;

public class OshiHWIDProvider implements LauncherHWIDInterface {
    public static SystemInfo systemInfo = new SystemInfo();
    public String getSerial()
    {
        try {
            return systemInfo.getHardware().getComputerSystem().getSerialNumber();
        } catch (Exception e)
        {
            LogHelper.error(e);
            return "";
        }

    }
    public String getProcessorID()
    {
        try {
            return systemInfo.getHardware().getProcessor().getProcessorID();
        } catch (Exception e)
        {
            LogHelper.error(e);
            return "";
        }

    }
    public String getHWDisk()
    {
        try {
            for (HWDiskStore s : systemInfo.getHardware().getDiskStores()) {
                if (!s.getModel().contains("USB"))
                    return s.getSerial();
            }
            return "";
        } catch (Exception e)
        {
            LogHelper.error(e);
            return "";
        }
    }
    public long getTotalMemory()
    {
        return systemInfo.getHardware().getMemory().getTotal();
    }
    public long getAvailableMemory()
    {
        return systemInfo.getHardware().getMemory().getAvailable();
    }

    @Override
    public HWID getHWID() {
        OshiHWID hwid = new OshiHWID();
        hwid.serialNumber = getSerial();
        hwid.totalMemory = getTotalMemory();
        hwid.HWDiskSerial = getHWDisk();
        hwid.processorID = getProcessorID();
        LogHelper.debug("serialNumber %s",hwid.serialNumber);
        LogHelper.debug("totalMemory %d",hwid.totalMemory);
        LogHelper.debug("HWDiskSerial %s",hwid.HWDiskSerial);
        LogHelper.debug("ProcessorID %s",hwid.processorID);
        return hwid;
    }
}
