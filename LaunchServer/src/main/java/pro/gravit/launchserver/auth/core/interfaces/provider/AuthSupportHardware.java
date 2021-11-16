package pro.gravit.launchserver.auth.core.interfaces.provider;

import pro.gravit.launcher.request.secure.HardwareReportRequest;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.auth.core.interfaces.UserHardware;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportHardware;
import pro.gravit.launchserver.helper.DamerauHelper;

import java.util.Arrays;

public interface AuthSupportHardware extends AuthSupport {
    UserHardware getHardwareInfoByPublicKey(byte[] publicKey);

    UserHardware getHardwareInfoByData(HardwareReportRequest.HardwareInfo info);

    UserHardware getHardwareInfoById(String id);

    UserHardware createHardwareInfo(HardwareReportRequest.HardwareInfo info, byte[] publicKey);

    void connectUserAndHardware(UserSession userSession, UserHardware hardware);

    void addPublicKeyToHardwareInfo(UserHardware hardware, byte[] publicKey);

    Iterable<User> getUsersByHardwareInfo(UserHardware hardware);

    void banHardware(UserHardware hardware);

    void unbanHardware(UserHardware hardware);

    default UserSupportHardware fetchUserHardware(User user) {
        return (UserSupportHardware) user;
    }

    default void normalizeHardwareInfo(HardwareReportRequest.HardwareInfo hardwareInfo) {
        if (hardwareInfo.baseboardSerialNumber != null)
            hardwareInfo.baseboardSerialNumber = hardwareInfo.baseboardSerialNumber.trim();
        if (hardwareInfo.hwDiskId != null) hardwareInfo.hwDiskId = hardwareInfo.hwDiskId.trim();
    }

    //Required normalize HardwareInfo
    default HardwareInfoCompareResult compareHardwareInfo(HardwareReportRequest.HardwareInfo first, HardwareReportRequest.HardwareInfo second) {
        HardwareInfoCompareResult result = new HardwareInfoCompareResult();
        if (first.hwDiskId == null || first.hwDiskId.isEmpty()) result.firstSpoofingLevel += 0.9;
        if (first.displayId == null || first.displayId.length < 4) result.firstSpoofingLevel += 0.3;
        if (first.baseboardSerialNumber == null || first.baseboardSerialNumber.trim().isEmpty())
            result.firstSpoofingLevel += 0.2;
        if (second.hwDiskId == null || second.hwDiskId.trim().isEmpty()) result.secondSpoofingLevel += 0.9;
        if (second.displayId == null || second.displayId.length < 4) result.secondSpoofingLevel += 0.3;
        if (second.baseboardSerialNumber == null || second.baseboardSerialNumber.trim().isEmpty())
            result.secondSpoofingLevel += 0.2;
        if (first.hwDiskId != null && second.hwDiskId != null) {
            int hwDIskIdRate = DamerauHelper.calculateDistance(first.hwDiskId.toLowerCase(), second.hwDiskId.toLowerCase());
            if (hwDIskIdRate == 0) // 100% compare
            {
                result.compareLevel += 0.99;
            } else if (hwDIskIdRate < 3) //Very small change
            {
                result.compareLevel += 0.85;
            } else if (hwDIskIdRate < (first.hwDiskId.length() + second.hwDiskId.length()) / 4) {
                double addLevel = hwDIskIdRate / ((double) (first.hwDiskId.length() + second.hwDiskId.length()) / 2.0);
                if (addLevel > 0.0 && addLevel < 0.85) result.compareLevel += addLevel;
            }
        }
        if (first.baseboardSerialNumber != null && second.baseboardSerialNumber != null) {
            int baseboardSerialRate = DamerauHelper.calculateDistance(first.baseboardSerialNumber.toLowerCase(), second.baseboardSerialNumber.toLowerCase());
            if (baseboardSerialRate == 0) // 100% compare
            {
                result.compareLevel += 0.3;
            } else if (baseboardSerialRate < 3) //Very small change
            {
                result.compareLevel += 0.15;
            }
        }
        if (first.displayId != null && second.displayId != null) {
            if (Arrays.equals(first.displayId, second.displayId)) {
                result.compareLevel += 0.75;
            }
        }
        //Check statistic info
        if (first.logicalProcessors == 0 || first.physicalProcessors == 0 || first.logicalProcessors < first.physicalProcessors) //WTF
            result.firstSpoofingLevel += 0.9;
        if (second.logicalProcessors == 0 || second.physicalProcessors == 0 || second.logicalProcessors < second.physicalProcessors) //WTF
            result.secondSpoofingLevel += 0.9;
        if (first.physicalProcessors == second.physicalProcessors && first.logicalProcessors == second.logicalProcessors)
            result.compareLevel += 0.05;
        if (first.battery != second.battery)
            result.compareLevel -= 0.05;
        if (first.processorMaxFreq == second.processorMaxFreq)
            result.compareLevel += 0.1;
        if (first.totalMemory == second.totalMemory)
            result.compareLevel += 0.1;
        if (Math.abs(first.totalMemory - second.totalMemory) < 32 * 1024)
            result.compareLevel += 0.05;
        return result;
    }

    class HardwareInfoCompareResult {
        public double firstSpoofingLevel = 0.0;
        public double secondSpoofingLevel = 0.0;
        public double compareLevel;
    }
}
