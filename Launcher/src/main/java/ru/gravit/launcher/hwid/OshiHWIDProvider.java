package ru.gravit.launcher.hwid;

import oshi.SystemInfo;
import oshi.hardware.*;
import ru.gravit.launcher.HWID;
import ru.gravit.launcher.LauncherHWIDInterface;
import ru.gravit.launcher.OshiHWID;
import ru.gravit.utils.helper.LogHelper;

import java.net.NetworkInterface;

public class OshiHWIDProvider implements LauncherHWIDInterface {
    public SystemInfo systemInfo;
    public HardwareAbstractionLayer hardware;
    public boolean noHWID;

    public OshiHWIDProvider() {
        try {
            systemInfo = new SystemInfo();
            noHWID = false;
        } catch (Throwable e) {
            LogHelper.error(e);
            noHWID = true;
        }
    }

    public String getSerial() {
        try {
            if (hardware == null) hardware = systemInfo.getHardware();
            return hardware.getComputerSystem().getSerialNumber();
        } catch (Exception e) {
            LogHelper.error(e);
            return "";
        }

    }

    public String getProcessorID() {
        try {
            if (hardware == null) hardware = systemInfo.getHardware();
            return hardware.getProcessor().getProcessorID();
        } catch (Exception e) {
            LogHelper.error(e);
            return "";
        }

    }

    public String getHWDisk() {
        try {
            if (hardware == null) hardware = systemInfo.getHardware();
            HWDiskStore store = null;
            long size = 0;
            for (HWDiskStore s : hardware.getDiskStores()) {
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

    public String getSoundCardInfo()
    {
        for(SoundCard soundcard : hardware.getSoundCards())
        {
            return soundcard.getName();
        }
        return "";
    }

    public String getMacAddr()
    {
        for(NetworkIF networkIF : hardware.getNetworkIFs())
        {
            for(String ipv4 : networkIF.getIPv4addr())
            {
                if(ipv4.startsWith("127.")) continue;
                if(ipv4.startsWith("10.")) continue;
                return networkIF.getMacaddr();
            }
        }
        return "";
    }

    public long getTotalMemory() {
        if (noHWID) return -1;
        if (hardware == null) hardware = systemInfo.getHardware();
        return hardware.getMemory().getTotal();
    }

    public long getAvailableMemory() {
        if (noHWID) return -1;
        if (hardware == null) hardware = systemInfo.getHardware();
        return hardware.getMemory().getAvailable();
    }

    public void printHardwareInformation() {
        try {
            if (hardware == null) hardware = systemInfo.getHardware();
            ComputerSystem computerSystem = hardware.getComputerSystem();
            LogHelper.debug("ComputerSystem Model: %s Serial: %s", computerSystem.getModel(), computerSystem.getSerialNumber());
            for (HWDiskStore s : hardware.getDiskStores()) {
                LogHelper.debug("HWDiskStore Serial: %s Model: %s Size: %d", s.getSerial(), s.getModel(), s.getSize());
            }
            for (UsbDevice s : hardware.getUsbDevices(true)) {
                LogHelper.debug("USBDevice Serial: %s Name: %s", s.getSerialNumber(), s.getName());
            }
            for(NetworkIF networkIF : hardware.getNetworkIFs())
            {
                LogHelper.debug("Network Interface: %s mac: %s", networkIF.getName(), networkIF.getMacaddr());
                NetworkInterface network = networkIF.getNetworkInterface();
                if(network.isLoopback() || network.isVirtual()) continue;
                LogHelper.debug("Network Interface display: %s name: %s", network.getDisplayName(), network.getName());
                for(String ipv4 : networkIF.getIPv4addr())
                {
                    if(ipv4.startsWith("127.")) continue;
                    if(ipv4.startsWith("10.")) continue;
                    LogHelper.subDebug("IPv4: %s", ipv4);
                }
            }
            for(SoundCard soundcard : hardware.getSoundCards())
            {
                 LogHelper.debug("SoundCard %s", soundcard.getName());
            }
            CentralProcessor processor = hardware.getProcessor();
            LogHelper.debug("Processor Model: %s ID: %s", processor.getModel(), processor.getProcessorID());
        } catch (Throwable e) {
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
        hwid.macAddr = getMacAddr();
        printHardwareInformation();
        return hwid;
    }
}
