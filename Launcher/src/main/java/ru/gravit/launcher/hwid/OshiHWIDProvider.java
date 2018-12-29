package ru.gravit.launcher.hwid;

import oshi.SystemInfo;
import oshi.hardware.*;
import ru.gravit.launcher.HWID;
import ru.gravit.launcher.LauncherHWIDInterface;
import ru.gravit.launcher.OshiHWID;
import ru.gravit.utils.helper.LogHelper;

public class OshiHWIDProvider implements LauncherHWIDInterface {
    public SystemInfo systemInfo;
    public boolean noHWID;
    public OshiHWIDProvider()
    {
        try {
            systemInfo = new SystemInfo();
            noHWID = false;
        } catch(Throwable e)
        {
            LogHelper.error(e);
            noHWID = true;
        }
    }

    public String getSerial() {
        try {
            return systemInfo.getHardware().getComputerSystem().getSerialNumber();
        } catch (Exception e) {
            LogHelper.error(e);
            return "";
        }

    }

    public String getProcessorID() {
        try {
            return systemInfo.getHardware().getProcessor().getProcessorID();
        } catch (Exception e) {
            LogHelper.error(e);
            return "";
        }

    }

    public String getHWDisk() {
        try {
            HWDiskStore store = null;
            long size = 0;
            for (HWDiskStore s : systemInfo.getHardware().getDiskStores()) {
                if (size < s.getSize()) {
                    store = s;
                    size = s.getSize();
                }
            }
            return store == null ? "" : store.getSerial();
        } catch (Exception e) {
            LogHelper.error(e);
            return "";
        }
    }

    public long getTotalMemory() {
        if(noHWID) return -1;
        return systemInfo.getHardware().getMemory().getTotal();
    }

    public long getAvailableMemory() {
        if(noHWID) return -1;
        return systemInfo.getHardware().getMemory().getAvailable();
    }

    public void printHardwareInformation() {
        try
        {
            HardwareAbstractionLayer hardware = systemInfo.getHardware();
            ComputerSystem computerSystem = hardware.getComputerSystem();
            LogHelper.debug("ComputerSystem Model: %s Serial: %s", computerSystem.getModel(), computerSystem.getSerialNumber());
            for (HWDiskStore s : systemInfo.getHardware().getDiskStores()) {
                LogHelper.debug("HWDiskStore Serial: %s Model: %s Size: %d", s.getSerial(), s.getModel(), s.getSize());
            }
            for (UsbDevice s : systemInfo.getHardware().getUsbDevices(true)) {
                LogHelper.debug("USBDevice Serial: %s Name: %s", s.getSerialNumber(), s.getName());
            }
            CentralProcessor processor = hardware.getProcessor();
            LogHelper.debug("Processor Model: %s ID: %s", processor.getModel(), processor.getProcessorID());
        } catch (Throwable e)
        {
            LogHelper.error(e);
        }

    }

    @Override
    public HWID getHWID() {
        OshiHWID hwid = new OshiHWID();
        hwid.serialNumber = getSerial();
        hwid.totalMemory = getTotalMemory();
        hwid.HWDiskSerial = getHWDisk();
        hwid.processorID = getProcessorID();
        printHardwareInformation();
        return hwid;
    }
}
