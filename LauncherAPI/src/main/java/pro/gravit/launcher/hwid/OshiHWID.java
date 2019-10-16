package pro.gravit.launcher.hwid;

import java.util.Objects;
import java.util.StringJoiner;

import com.google.gson.Gson;

import pro.gravit.launcher.LauncherAPI;

public class OshiHWID implements HWID {
    public static Gson gson = new Gson();
    @LauncherAPI
    public long totalMemory = 0;
    @LauncherAPI
    public String serialNumber;
    @LauncherAPI
    public String HWDiskSerial;
    @LauncherAPI
    public String processorID;
    @LauncherAPI
    public String macAddr;

    @Override
    public int getLevel() //Уровень доверия, насколько уникальные значения
    {
        int result = 0;
        if (totalMemory != 0) result += 32;
        if (serialNumber != null) result += isRealSerialNumber() ? 20 : 3;
        if (HWDiskSerial != null && !HWDiskSerial.isEmpty()) result += 38;
        if (processorID != null && !processorID.isEmpty()) result += 15;
        if (macAddr != null  && !macAddr.isEmpty()) result += 25;
        return result;
    }

    @Override
    public int getAntiLevel() {
        return HWIDCheckHelper.checkString(serialNumber) + HWIDCheckHelper.checkString(HWDiskSerial);
    }

    @Override
    public int compare(HWID hwid) {
        if (hwid instanceof OshiHWID) {
            int rate = 0;
            OshiHWID oshi = (OshiHWID) hwid;
            if (Math.abs(oshi.totalMemory - totalMemory) < 1024 * 1024) rate += 5;
            if (oshi.totalMemory == totalMemory) rate += 32;
            if (oshi.HWDiskSerial.equals(HWDiskSerial) && !HWDiskSerial.isEmpty()) rate += 38;
            if (oshi.processorID.equals(processorID) && !processorID.isEmpty()) rate += 15;
            if (oshi.serialNumber.equals(serialNumber)) rate += isRealSerialNumber() ? 20 : 3;
            if (!oshi.macAddr.isEmpty() && oshi.macAddr.equals(macAddr)) rate += 19;
            return rate;
        }
        return 0;
    }

    @Override
    public boolean isNull() {
        return getLevel() < 15;
    }

    @Override
    public void normalize() {
        HWDiskSerial = HWDiskSerial.trim();
        serialNumber = serialNumber.trim();
        processorID = processorID.trim();
        macAddr = macAddr.trim();
    }
    public boolean isRealSerialNumber()
    {
        if(serialNumber.isEmpty()) return false;
        if(serialNumber.equals("System Serial Number")) return false;
        if(serialNumber.equals("To be filled by O.E.M.")) return false;
        if(serialNumber.equals("unknown")) return false;
        if(serialNumber.equals("None")) return false;
        if(serialNumber.equals("Default string")) return false;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OshiHWID oshiHWID = (OshiHWID) o;
        return totalMemory == oshiHWID.totalMemory &&
                Objects.equals(serialNumber, oshiHWID.serialNumber) &&
                Objects.equals(HWDiskSerial, oshiHWID.HWDiskSerial) &&
                Objects.equals(processorID, oshiHWID.processorID) &&
                Objects.equals(macAddr, oshiHWID.macAddr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalMemory, serialNumber, HWDiskSerial, processorID, macAddr);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OshiHWID.class.getSimpleName() + "[", "]")
                .add("totalMemory=" + totalMemory)
                .add("serialNumber='" + serialNumber + "'")
                .add("HWDiskSerial='" + HWDiskSerial + "'")
                .add("processorID='" + processorID + "'")
                .add("macAddr='" + macAddr + "'")
                .toString();
    }
}
