package ru.gravit.launcher;

import com.google.gson.Gson;

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
    public String getSerializeString() {
        return gson.toJson(this);
    }

    @Override
    public int getLevel() //Уровень доверия, насколько уникальные значения
    {
        int result = 0;
        if (totalMemory != 0) result+=8;
        if (serialNumber != null && !serialNumber.equals("unknown")) result += 12;
        if (HWDiskSerial != null && !HWDiskSerial.equals("unknown")) result += 30;
        if (processorID != null && !processorID.equals("unknown")) result += 10;
        if (macAddr != null && !macAddr.equals("00:00:00:00:00:00")) result += 15;
        return result;
    }

    @Override
    public int compare(HWID hwid) {
        if(hwid instanceof OshiHWID)
        {
            int rate = 0;
            OshiHWID oshi = (OshiHWID) hwid;
            if(Math.abs(oshi.totalMemory - totalMemory) < 1024*1024) rate+=5;
            if(oshi.totalMemory == totalMemory) rate+=15;
            if(oshi.HWDiskSerial.equals(HWDiskSerial)) rate+=45;
            if(oshi.processorID.equals(processorID)) rate+=18;
            if(oshi.serialNumber.equals(serialNumber)) rate+=15;
            if(!oshi.macAddr.isEmpty() && oshi.macAddr.equals(macAddr)) rate+=19;
            return rate;
        }
        return 0;
    }

    @Override
    public boolean isNull() {
        return getLevel() < 15;
    }
}
