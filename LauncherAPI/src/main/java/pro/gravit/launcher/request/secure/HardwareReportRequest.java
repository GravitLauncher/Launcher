package pro.gravit.launcher.request.secure;

import pro.gravit.launcher.events.request.HardwareReportRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.utils.helper.SecurityHelper;

import java.util.Arrays;

public class HardwareReportRequest extends Request<HardwareReportRequestEvent> {
    public HardwareInfo hardware;

    @Override
    public String getType() {
        return "hardwareReport";
    }

    public static class HardwareInfo {
        public int bitness;
        public long totalMemory;
        public int logicalProcessors;
        public int physicalProcessors;
        public long processorMaxFreq;
        public boolean battery;
        public String hwDiskId;
        public byte[] displayId;
        public String baseboardSerialNumber;
        public String graphicCard;

        @Override
        public String toString() {
            return "HardwareInfo{" +
                    "bitness=" + bitness +
                    ", totalMemory=" + totalMemory +
                    ", logicalProcessors=" + logicalProcessors +
                    ", physicalProcessors=" + physicalProcessors +
                    ", processorMaxFreq=" + processorMaxFreq +
                    ", battery=" + battery +
                    ", hwDiskId='" + hwDiskId + '\'' +
                    ", displayId=" + (displayId == null ? null : SecurityHelper.toHex(displayId)) +
                    ", baseboardSerialNumber='" + baseboardSerialNumber + '\'' +
                    ", graphicCard='" + graphicCard + '\'' +
                    '}';
        }
    }
}
