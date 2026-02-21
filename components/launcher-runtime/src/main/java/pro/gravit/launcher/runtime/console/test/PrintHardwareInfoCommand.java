package pro.gravit.launcher.runtime.console.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.runtime.utils.HWIDProvider;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class PrintHardwareInfoCommand extends Command {

    private static final Logger logger =
            LoggerFactory.getLogger(PrintHardwareInfoCommand.class);

    @Override
    public String getArgsDescription() {
        return "[]";
    }

    @Override
    public String getUsageDescription() {
        return "print your hardware info and timings";
    }

    @Override
    public void invoke(String... args) {
        logger.info("Your Hardware ID:");
        long startTime = System.currentTimeMillis();
        long currentTime;
        HWIDProvider provider = new HWIDProvider();
        currentTime = System.currentTimeMillis();
        logger.info("Create HWIDProvider instance: {} ms", currentTime - startTime);
        startTime = System.currentTimeMillis();
        int bitness = provider.getBitness();
        long totalMemory = provider.getTotalMemory();
        boolean isBattery = provider.isBattery();
        currentTime = System.currentTimeMillis();
        logger.info("Bitness: {}, totalMemory: {}({} GB), battery {}, TIME: {} ms", bitness, totalMemory, String.format("%.3f", (double) totalMemory / (1024.0 * 1024.0 * 1024.0)), Boolean.toString(isBattery), currentTime - startTime);
        startTime = System.currentTimeMillis();
        int logicalProcessors = provider.getProcessorLogicalCount();
        int physicalProcessors = provider.getProcessorPhysicalCount();
        long processorMaxFreq = provider.getProcessorMaxFreq();
        currentTime = System.currentTimeMillis();
        logger.info("Processors || logical: {} physical {} freq {}, TIME: {} ms", logicalProcessors, physicalProcessors, processorMaxFreq, currentTime - startTime);
        startTime = System.currentTimeMillis();
        String hwDiskID = provider.getHWDiskID();
        currentTime = System.currentTimeMillis();
        logger.info("HWDiskID {}, TIME: {} ms", hwDiskID, currentTime - startTime);
        startTime = System.currentTimeMillis();
        String baseboardSerial = provider.getBaseboardSerialNumber();
        currentTime = System.currentTimeMillis();
        logger.info("BaseboardSerial {}, TIME: {} ms", baseboardSerial, currentTime - startTime);
        startTime = System.currentTimeMillis();
        String graphicCardName = provider.getGraphicCardName();
        long graphicCardVRam = provider.getGraphicCardMemory();
        currentTime = System.currentTimeMillis();
        logger.info("GraphicCard {} ({} VRAM), TIME: {} ms", graphicCardName, String.format("%.3f", (double) graphicCardVRam), currentTime - startTime);
        logger.info("Hardware ID end");
    }
}