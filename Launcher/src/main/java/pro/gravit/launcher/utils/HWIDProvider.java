package pro.gravit.launcher.utils;

import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;
import pro.gravit.launcher.request.secure.HardwareReportRequest;

import java.util.List;

public class HWIDProvider {
    public final SystemInfo systemInfo;
    public final OperatingSystem system;
    public final HardwareAbstractionLayer hardware;

    public HWIDProvider() {
        systemInfo = new SystemInfo();
        system = systemInfo.getOperatingSystem();
        hardware = systemInfo.getHardware();
    }

    //Statistic information
    public int getBitness() {
        return system.getBitness();
    }

    public long getTotalMemory() {
        return hardware.getMemory().getTotal();
    }

    public long getProcessorMaxFreq() {
        return hardware.getProcessor().getMaxFreq();
    }

    public int getProcessorPhysicalCount() {
        return hardware.getProcessor().getPhysicalProcessorCount();
    }

    public int getProcessorLogicalCount() {
        return hardware.getProcessor().getLogicalProcessorCount();
    }

    public boolean isBattery() {
        List<PowerSource> powerSources = hardware.getPowerSources();
        return powerSources != null && powerSources.size() != 0;
    }

    //Hardware Information
    public String getHWDiskID() {
        List<HWDiskStore> hwDiskStore = hardware.getDiskStores();
        long size = 0;
        HWDiskStore maxStore = null;
        for (HWDiskStore store : hwDiskStore) {
            if (store.getSize() > size) {
                maxStore = store;
                size = store.getSize();
            }
        }
        if (maxStore != null) {
            return maxStore.getSerial();
        }
        return null;
    }

    public GraphicsCard getGraphicCard() {
        List<GraphicsCard> graphicsCards = hardware.getGraphicsCards();
        GraphicsCard result = null;
        long size = 0;
        for (GraphicsCard card : graphicsCards) {
            long vram = card.getVRam();
            if (vram > size) {
                result = card;
                size = vram;
            }
        }
        return result;
    }

    public String getGraphicCardName() {
        GraphicsCard card = getGraphicCard();
        if (card == null) {
            return null;
        }
        return card.getName();
    }

    public long getGraphicCardMemory() {
        GraphicsCard card = getGraphicCard();
        if (card == null) {
            return 0;
        }
        return card.getVRam();
    }

    public byte[] getDisplayID() {
        List<Display> displays = hardware.getDisplays();
        if (displays == null || displays.size() == 0) return null;
        for (Display display : displays) {
            return display.getEdid();
        }
        return null;
    }

    public String getBaseboardSerialNumber() {
        return hardware.getComputerSystem().getBaseboard().getSerialNumber();
    }

    public HardwareReportRequest.HardwareInfo getHardwareInfo(boolean needSerial) {
        HardwareReportRequest.HardwareInfo info = new HardwareReportRequest.HardwareInfo();
        info.bitness = getBitness();
        info.logicalProcessors = getProcessorLogicalCount();
        info.physicalProcessors = getProcessorPhysicalCount();
        info.processorMaxFreq = getProcessorMaxFreq();
        info.totalMemory = getTotalMemory();
        info.battery = isBattery();
        info.graphicCard = getGraphicCardName();
        if (needSerial) {
            info.hwDiskId = getHWDiskID();
            info.displayId = getDisplayID();
            info.baseboardSerialNumber = getBaseboardSerialNumber();
        }
        return info;
    }
}
