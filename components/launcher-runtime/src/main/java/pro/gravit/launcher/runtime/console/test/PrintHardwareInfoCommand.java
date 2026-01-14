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
        logger.info("Bitness: {}, totalMemory: %d(%.3f GB), battery %s, TIME: %d ms", bitness, totalMemory, (double) totalMemory / (1024.0 * 1024.0 * 1024.0), Boolean.toString(isBattery), currentTime - startTime);
        startTime = System.currentTimeMillis();
        int logicalProcessors = provider.getProcessorLogicalCount();
        int physicalProcessors = provider.getProcessorPhysicalCount();
        long processorMaxFreq = provider.getProcessorMaxFreq();
        currentTime = System.currentTimeMillis();
        logger.info("Processors || logical: {} physical {} freq {}, TIME: %d ms", logicalProcessors, physicalProcessors, processorMaxFreq, currentTime - startTime);
        startTime = System.currentTimeMillis();
        String hwDiskID = provider.getHWDiskID();
        currentTime = System.currentTimeMillis();
        logger.info("HWDiskID {}, TIME: %d ms", hwDiskID, currentTime - startTime);
        startTime = System.currentTimeMillis();
        String baseboardSerial = provider.getBaseboardSerialNumber();
        currentTime = System.currentTimeMillis();
        logger.info("BaseboardSerial {}, TIME: %d ms", baseboardSerial, currentTime - startTime);
        startTime = System.currentTimeMillis();
        String graphicCardName = provider.getGraphicCardName();
        long graphicCardVRam = provider.getGraphicCardMemory();
        currentTime = System.currentTimeMillis();
        logger.info("GraphicCard {} (%.3f vram), TIME: %d ms", graphicCardName, (double) graphicCardVRam, currentTime - startTime);
        logger.info("Hardware ID end");
    }
}